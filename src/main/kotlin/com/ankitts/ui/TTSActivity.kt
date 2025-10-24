package com.ankitts.exltotts.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ankitts.exltotts.databinding.ActivityTtsBinding
import com.ankitts.exception.TTSException
import com.ankitts.model.ProcessedData
import com.ankitts.tts.TTSManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * TTSActivity to orchestrate the TTS conversion process with UI feedback.
 * This activity receives ProcessedData and outputDirectoryUri from PreviewActivity,
 * initializes TTSManager, and performs the conversion with progress tracking.
 */
class TTSActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTtsBinding
    private lateinit var processedData: ProcessedData
    private lateinit var outputDirectoryUri: String
    private lateinit var ttsManager: TTSManager
    private var conversionJob: Job? = null
    private val logBuilder = StringBuilder()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTtsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup action bar
        supportActionBar?.apply {
            title = getString(com.ankitts.exltotts.R.string.tts_activity_title)
            setDisplayHomeAsUpEnabled(true)
        }
        
        // Extract Intent data
        processedData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PROCESSED_DATA", ProcessedData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<ProcessedData>("PROCESSED_DATA")
        } ?: run {
            showErrorAndFinish("No processed data received")
            return
        }
        
        outputDirectoryUri = intent.getStringExtra("OUTPUT_DIRECTORY_URI") ?: run {
            showErrorAndFinish("No output directory URI received")
            return
        }
        
        // Initialize TTSManager
        ttsManager = TTSManager(this)
        
        // Setup Cancel button
        binding.btnCancel.setOnClickListener {
            cancelConversion()
        }
        
        // Start initialization and conversion
        initializeAndStartConversion()
    }
    
    /**
     * Initialize TTS and start conversion process.
     */
    private fun initializeAndStartConversion() {
        updateStatus(getString(com.ankitts.exltotts.R.string.tts_initializing))
        binding.btnCancel.isEnabled = false
        
        ttsManager.initialize(
            onSuccess = {
                runOnUiThread {
                    startConversion()
                }
            },
            onError = { error ->
                runOnUiThread {
                    showErrorDialog(error)
                    binding.btnCancel.isEnabled = true
                    binding.btnCancel.text = getString(com.ankitts.exltotts.R.string.tts_cancel_button)
                }
            }
        )
    }
    
    /**
     * Start the conversion process.
     */
    private fun startConversion() {
        val audioSpecs = processedData.audioSpecs
        
        if (audioSpecs.isEmpty()) {
            showErrorDialog("No audio files to convert")
            return
        }
        
        // Setup progress bar
        binding.progressBar.max = audioSpecs.size
        binding.progressBar.progress = 0
        
        updateStatus(getString(com.ankitts.exltotts.R.string.tts_progress_message, 0, audioSpecs.size))
        binding.btnCancel.isEnabled = true
        
        // Start conversion in coroutine
        conversionJob = lifecycleScope.launch {
            try {
                val result = ttsManager.convertToAudioFiles(
                    audioSpecs = audioSpecs,
                    outputDirectoryUri = Uri.parse(outputDirectoryUri),
                    progressCallback = { current, total, fileName ->
                        withContext(Dispatchers.Main) {
                            updateProgress(current, total, fileName)
                        }
                    },
                    errorCallback = { fileName, error ->
                        withContext(Dispatchers.Main) {
                            appendToLog("ERROR - $fileName: $error")
                        }
                    }
                )
                
                withContext(Dispatchers.Main) {
                    onConversionComplete(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog("Conversion failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Update progress UI elements.
     */
    private fun updateProgress(current: Int, total: Int, fileName: String) {
        binding.progressBar.progress = current
        binding.tvProgressText.text = getString(com.ankitts.exltotts.R.string.tts_files_completed, current, total)
        binding.tvCurrentFile.text = getString(com.ankitts.exltotts.R.string.tts_current_file, fileName)
        binding.tvStatus.text = getString(com.ankitts.exltotts.R.string.tts_progress_message, current, total)
        
        appendToLog("Processing: $fileName")
    }
    
    /**
     * Update status message.
     */
    private fun updateStatus(message: String) {
        binding.tvStatus.text = message
    }
    
    /**
     * Append message to log.
     */
    private fun appendToLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logBuilder.append("[$timestamp] $message\n")
        binding.tvLog.text = logBuilder.toString()
        binding.scrollViewLog.fullScroll(View.FOCUS_DOWN)
    }
    
    /**
     * Handle conversion completion.
     */
    private fun onConversionComplete(result: TTSManager.ConversionResult) {
        val statusMessage = if (result.failureCount == 0) {
            getString(com.ankitts.exltotts.R.string.tts_conversion_complete)
        } else {
            getString(com.ankitts.exltotts.R.string.tts_conversion_failed)
        }
        
        updateStatus(statusMessage)
        binding.progressBar.progress = binding.progressBar.max
        
        // Set activity result before showing dialog
        val resultIntent = Intent().apply {
            putExtra("CONVERSION_SUCCESS", result.failureCount == 0)
            putExtra("SUCCESS_COUNT", result.successCount)
            putExtra("FAILURE_COUNT", result.failureCount)
        }
        setResult(RESULT_OK, resultIntent)
        
        // Update button
        binding.btnCancel.text = "Close"
        binding.btnCancel.setOnClickListener { finish() }
        
        // Append summary to log
        appendToLog("Conversion complete. Success: ${result.successCount}, Failed: ${result.failureCount}")
        if (result.failedFiles.isNotEmpty()) {
            appendToLog("Failed files: ${result.failedFiles.joinToString(", ")}")
        }
        
        // Show completion dialog
        showCompletionDialog(result)
    }
    
    /**
     * Show completion dialog with results.
     */
    private fun showCompletionDialog(result: TTSManager.ConversionResult) {
        val title = if (result.failureCount == 0) {
            "Conversion Complete"
        } else {
            "Conversion Completed with Errors"
        }
        
        val message = buildString {
            append("Successfully converted: ${result.successCount} files\n")
            if (result.failureCount > 0) {
                append("Failed: ${result.failureCount} files\n")
                append("Failed files: ${result.failedFiles.joinToString(", ")}")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> 
                finish() 
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Cancel the conversion process.
     */
    private fun cancelConversion() {
        if (conversionJob?.isActive == true) {
            AlertDialog.Builder(this)
                .setTitle("Cancel Conversion")
                .setMessage("Are you sure you want to cancel the conversion?")
                .setPositiveButton("Yes") { _, _ ->
                    conversionJob?.cancel()
                    ttsManager.shutdown()
                    updateStatus("Conversion cancelled")
                    appendToLog("Conversion cancelled by user")
                    binding.btnCancel.text = "Close"
                    binding.btnCancel.setOnClickListener { finish() }
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            finish()
        }
    }
    
    /**
     * Show error dialog and finish activity.
     */
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show error and finish activity.
     */
    private fun showErrorAndFinish(message: String) {
        showErrorDialog(message)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        conversionJob?.cancel()
        ttsManager.shutdown()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        if (conversionJob?.isActive == true) {
            AlertDialog.Builder(this)
                .setTitle("Cancel Conversion")
                .setMessage("Conversion is in progress. Are you sure you want to cancel?")
                .setPositiveButton("Yes") { _, _ ->
                    cancelConversion()
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            finish()
        }
        return true
    }
}


