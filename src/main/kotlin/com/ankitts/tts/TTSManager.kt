package com.ankitts.tts

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.documentfile.provider.DocumentFile
import com.ankitts.exception.TTSException
import com.ankitts.model.AudioFileSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CancellationException

/**
 * Core TTSManager class responsible for all Text-to-Speech operations.
 * This class is designed as a lifecycle-aware component that can be initialized,
 * used for conversions, and properly shut down.
 */
class TTSManager(private val context: Context) {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized: Boolean = false
    private var selectedVoice: Voice? = null
    
    // Global utterance progress tracking
    private var currentUtteranceId: String? = null
    private var currentContinuation: kotlin.coroutines.Continuation<Boolean>? = null
    private var globalUtteranceListener: UtteranceProgressListener? = null
    
    /**
     * Initialize the TTS engine with proper configuration.
     * 
     * @param onSuccess Callback invoked when initialization succeeds
     * @param onError Callback invoked when initialization fails with error message
     */
    fun initialize(onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        // Set language to American English
                        val result = textToSpeech?.setLanguage(Locale.US)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            onError("Language not supported or missing data")
                            return@TextToSpeech
                        }
                        
                        // Select random voice
                        selectRandomVoice()
                        
                        // Set speech rate
                        textToSpeech?.setSpeechRate(0.6f)
                        
                        // Set up global utterance progress listener
                        setupGlobalUtteranceListener()
                        
                        isInitialized = true
                        onSuccess()
                    } catch (e: Exception) {
                        onError("Failed to configure TTS: ${e.message}")
                    }
                } else {
                    onError("Failed to initialize Text-to-Speech")
                }
            }
        } catch (e: Exception) {
            onError("TTS initialization error: ${e.message}")
        }
    }
    
    /**
     * Select a random voice from available local US voices.
     * Filters voices to include only local, high-quality US English voices.
     */
    private fun selectRandomVoice() {
        val voices = textToSpeech?.voices ?: emptyList()
        
        val suitableVoices = voices.filter { voice ->
            voice.locale.language == "en" && 
            voice.locale.country == "US" &&
            !voice.isNetworkConnectionRequired &&
            voice.quality >= Voice.QUALITY_NORMAL
        }
        
        if (suitableVoices.isEmpty()) {
            throw TTSException("No suitable voices found")
        }
        
        selectedVoice = suitableVoices.random()
        textToSpeech?.voice = selectedVoice
        
        android.util.Log.d("TTSManager", "Selected voice: ${selectedVoice?.name}")
    }
    
    /**
     * Set up the global utterance progress listener that guards by utteranceId.
     * This listener is registered once at initialization and routes events by ID to avoid races.
     */
    private fun setupGlobalUtteranceListener() {
        globalUtteranceListener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                android.util.Log.d("TTSManager", "Started synthesis for: $utteranceId")
            }
            
            override fun onDone(utteranceId: String?) {
                android.util.Log.d("TTSManager", "Completed synthesis for: $utteranceId")
                // Guard by utteranceId to avoid races
                if (utteranceId == currentUtteranceId && currentContinuation != null) {
                    val continuation = currentContinuation
                    currentContinuation = null
                    currentUtteranceId = null
                    continuation?.resume(true)
                }
            }
            
            override fun onError(utteranceId: String?) {
                android.util.Log.e("TTSManager", "Synthesis error for: $utteranceId")
                // Guard by utteranceId to avoid races
                if (utteranceId == currentUtteranceId && currentContinuation != null) {
                    val continuation = currentContinuation
                    currentContinuation = null
                    currentUtteranceId = null
                    continuation?.resume(false)
                }
            }
        }
        
        textToSpeech?.setOnUtteranceProgressListener(globalUtteranceListener)
    }
    
    /**
     * Convert a list of AudioFileSpec objects to audio files.
     * 
     * @param audioSpecs List of audio specifications to convert
     * @param outputDirectoryUri URI of the output directory
     * @param progressCallback Callback for progress updates (current, total, fileName)
     * @param errorCallback Callback for error notifications (fileName, error)
     * @return ConversionResult with success/failure counts and failed file names
     */
    suspend fun convertToAudioFiles(
        audioSpecs: List<AudioFileSpec>,
        outputDirectoryUri: Uri,
        progressCallback: (current: Int, total: Int, fileName: String) -> Unit,
        errorCallback: (fileName: String, error: String) -> Unit
    ): ConversionResult = withContext(Dispatchers.IO) {
        
        if (!isInitialized) {
            throw TTSException("TTS not initialized")
        }
        
        if (outputDirectoryUri == Uri.EMPTY) {
            throw TTSException("Invalid output directory URI")
        }
        
        val documentFile = DocumentFile.fromTreeUri(context, outputDirectoryUri)
        if (documentFile == null || !documentFile.exists()) {
            throw TTSException("Output directory not accessible")
        }
        
        var successCount = 0
        var failureCount = 0
        val failedFiles = mutableListOf<String>()
        
        audioSpecs.forEachIndexed { index, spec ->
            try {
                progressCallback(index + 1, audioSpecs.size, spec.fileName)
                
                val audioFileResult = createAudioFile(documentFile, spec.fileName)
                if (audioFileResult == null) {
                    errorCallback(spec.fileName, "Failed to create output file")
                    failureCount++
                    failedFiles.add(spec.fileName)
                    return@forEachIndexed
                }
                
                val success = try {
                    synthesizeSingleFile(spec, audioFileResult.tempFile)
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    val timeoutSeconds = calculateTimeout(spec.textToConvert.length) / 1000
                    errorCallback(spec.fileName, "Synthesis timeout (${timeoutSeconds}s)")
                    false
                } catch (e: CancellationException) {
                    errorCallback(spec.fileName, "Synthesis cancelled")
                    false
                } catch (e: Exception) {
                    errorCallback(spec.fileName, "Synthesis error: ${e.message}")
                    false
                }
                
                if (success) {
                    // Copy from temp file to destination DocumentFile
                    try {
                        val inputStream: InputStream = FileInputStream(audioFileResult.tempFile)
                        inputStream.use { input ->
                            val outputStream: OutputStream? = context.contentResolver.openOutputStream(audioFileResult.documentFile.uri)
                            
                            if (outputStream != null) {
                                outputStream.use { output ->
                                    input.copyTo(output)
                                    output.flush()
                                }
                                
                                // Delete temp file
                                audioFileResult.tempFile.delete()
                                
                                successCount++
                            } else {
                                errorCallback(spec.fileName, "Failed to open output stream for destination file")
                                failureCount++
                                failedFiles.add(spec.fileName)
                                // Clean up temp file
                                audioFileResult.tempFile.delete()
                            }
                        }
                    } catch (e: Exception) {
                        errorCallback(spec.fileName, "Failed to copy audio file: ${e.message}")
                        failureCount++
                        failedFiles.add(spec.fileName)
                        // Clean up temp file
                        audioFileResult.tempFile.delete()
                    }
                } else {
                    failureCount++
                    failedFiles.add(spec.fileName)
                    // Clean up temp file
                    audioFileResult.tempFile.delete()
                }
                
            } catch (e: Exception) {
                errorCallback(spec.fileName, "Error: ${e.message}")
                failureCount++
                failedFiles.add(spec.fileName)
            }
        }
        
        ConversionResult(successCount, failureCount, failedFiles)
    }
    
    /**
     * Create an audio file in the output directory.
     * Uses a two-step process: temp file -> DocumentFile due to TTS API requirements.
     */
    private fun createAudioFile(parentDir: DocumentFile, fileName: String): AudioFileResult? {
        return try {
            // Normalize fileName to ensure it ends with .wav extension
            val normalizedFileName = if (fileName.endsWith(".wav", ignoreCase = true)) {
                fileName
            } else {
                "$fileName.wav"
            }
            
            val documentFile = parentDir.createFile("audio/wav", normalizedFileName)
            if (documentFile == null) return null
            
            // Create temp file for TTS synthesis using normalized filename
            val tempFile = File(context.cacheDir, "temp_$normalizedFileName")
            AudioFileResult(documentFile, tempFile)
        } catch (e: IOException) {
            android.util.Log.e("TTSManager", "Failed to create audio file: ${e.message}")
            null
        }
    }
    
    /**
     * Calculate timeout duration based on text length.
     * Uses a base timeout plus per-character budget, capped at a reasonable maximum.
     * 
     * @param textLength Length of text to be synthesized
     * @return Timeout duration in milliseconds
     */
    private fun calculateTimeout(textLength: Int): Long {
        // Base timeout of 5 seconds for setup and processing
        val baseTimeoutMs = 5000L
        
        // Per-character budget: 50ms per character (allows for ~20 chars/second)
        val perCharBudgetMs = 50L
        
        // Calculate total timeout
        val calculatedTimeout = baseTimeoutMs + (textLength * perCharBudgetMs)
        
        // Cap at 5 minutes (300 seconds) to prevent extremely long timeouts
        val maxTimeoutMs = 300000L
        
        return minOf(calculatedTimeout, maxTimeoutMs)
    }

    /**
     * Synthesize a single file using TTS engine.
     * Uses coroutines to wait for synthesis completion with length-aware timeout and cancellation support.
     * Uses the global utterance progress listener with utteranceId guards to avoid races.
     */
    private suspend fun synthesizeSingleFile(spec: AudioFileSpec, outputFile: File): Boolean = withTimeout(calculateTimeout(spec.textToConvert.length)) {
        suspendCancellableCoroutine { continuation ->
            val utteranceId = UUID.randomUUID().toString()
            val bundle = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            
            // Set current utterance tracking for global listener
            currentUtteranceId = utteranceId
            currentContinuation = continuation
            
            // Configure TTS engine with AudioFileSpec properties
            configureTTSForSpec(spec)
            
            // Register cancellation handler to stop TTS synthesis
            continuation.invokeOnCancellation {
                android.util.Log.d("TTSManager", "Cancellation requested for synthesis: $utteranceId")
                textToSpeech?.stop()
                // Clear current tracking on cancellation
                if (currentUtteranceId == utteranceId) {
                    currentUtteranceId = null
                    currentContinuation = null
                }
            }
            
            val result = textToSpeech?.synthesizeToFile(spec.textToConvert, bundle, outputFile, utteranceId)
            if (result == TextToSpeech.ERROR) {
                // Clear current tracking on immediate error
                if (currentUtteranceId == utteranceId) {
                    currentUtteranceId = null
                    currentContinuation = null
                }
                continuation.resume(false)
            }
        }
    }
    
    /**
     * Configure TTS engine with AudioFileSpec properties.
     * Sets language and speech rate from the spec, with fallback to defaults when invalid.
     */
    private fun configureTTSForSpec(spec: AudioFileSpec) {
        // Configure language from AudioFileSpec
        try {
            val locale = Locale.forLanguageTag(spec.language)
            val result = textToSpeech?.setLanguage(locale)
            
            // If language is not supported, fall back to default (US English)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                android.util.Log.w("TTSManager", "Language ${spec.language} not supported, falling back to US English")
                textToSpeech?.setLanguage(Locale.US)
            } else {
                android.util.Log.d("TTSManager", "Set language to: ${spec.language}")
            }
        } catch (e: Exception) {
            android.util.Log.w("TTSManager", "Invalid language format: ${spec.language}, falling back to US English")
            textToSpeech?.setLanguage(Locale.US)
        }
        
        // Configure speech rate from AudioFileSpec
        val speechRate = when {
            spec.speechRate > 0f && spec.speechRate <= 3.0f -> spec.speechRate
            else -> {
                android.util.Log.w("TTSManager", "Invalid speech rate: ${spec.speechRate}, falling back to 0.6f")
                0.6f
            }
        }
        
        textToSpeech?.setSpeechRate(speechRate)
        android.util.Log.d("TTSManager", "Set speech rate to: $speechRate")
    }
    
    /**
     * Shutdown the TTS engine and release resources.
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        globalUtteranceListener = null
        currentUtteranceId = null
        currentContinuation = null
        
        // Clean up any remaining temporary files in cache directory
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") && file.name.endsWith(".wav")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TTSManager", "Error cleaning up temp files during shutdown", e)
        }
        
        android.util.Log.d("TTSManager", "TTS shutdown completed")
    }
    
    /**
     * Data class to encapsulate conversion results.
     */
    data class ConversionResult(
        val successCount: Int,
        val failureCount: Int,
        val failedFiles: List<String>
    )
    
    /**
     * Holder object for audio file creation results.
     * Contains both the destination DocumentFile and the temporary File for synthesis.
     */
    data class AudioFileResult(
        val documentFile: DocumentFile,
        val tempFile: File
    )
}


