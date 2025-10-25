package com.ankitts.exltotts.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.ankitts.exltotts.databinding.ActivityMainBinding
import com.ankitts.exltotts.R
import com.ankitts.exception.ExcelParsingException
import com.ankitts.exception.InvalidRowRangeException
import com.ankitts.model.ProcessedData
import com.ankitts.parser.ExcelParser
import com.ankitts.processor.DataProcessor
import com.ankitts.util.UriHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/**
 * Main activity for the Excel to TTS application.
 * Handles file selection, directory selection, and Excel processing.
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var selectedFileUri: Uri? = null
    private var selectedDirectoryUri: Uri? = null
    
    // Activity result launchers for file and directory selection
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            handleFileSelection(uri)
        }
    }
    
    private val directoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            handleDirectorySelection(uri)
        }
    }
    
    private val previewActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val conversionSuccess = data?.getBooleanExtra("CONVERSION_SUCCESS", false) ?: false
            val successCount = data?.getIntExtra("SUCCESS_COUNT", 0) ?: 0
            val failureCount = data?.getIntExtra("FAILURE_COUNT", 0) ?: 0
            
            if (conversionSuccess) {
                Toast.makeText(this, getString(R.string.operation_successful), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, getString(R.string.operation_successful_with_errors, successCount, failureCount), Toast.LENGTH_LONG).show()
            }
            
            // Reset app state after successful conversion
            resetAppState()
        } else {
            // Handle cancellation or failure - keep selections for retry
            Toast.makeText(this, getString(R.string.conversion_cancelled_message), Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        validateInputs()
    }
    
    /**
     * Sets up click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        binding.btnSelectFile.setOnClickListener {
            launchFilePicker()
        }
        
        binding.btnSelectDirectory.setOnClickListener {
            launchDirectoryPicker()
        }
        
        binding.btnProcess.setOnClickListener {
            processExcelFile()
        }
        
        // Add text change listeners to validate inputs in real-time
        binding.etStartRow.doOnTextChanged { _, _, _, _ -> validateInputs() }
        binding.etEndRow.doOnTextChanged { _, _, _, _ -> validateInputs() }
    }
    
    /**
     * Launches the file picker with Excel file filter
     */
    private fun launchFilePicker() {
        filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    }
    
    /**
     * Launches the directory picker for output directory selection
     */
    private fun launchDirectoryPicker() {
        directoryPickerLauncher.launch(null)
    }
    
    /**
     * Handles file selection result
     */
    private fun handleFileSelection(uri: Uri) {
        try {
            // Take persistable URI permission
            contentResolver.takePersistableUriPermission(
                uri, 
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            // Validate Excel file
            if (!UriHelper.validateExcelFile(this, uri)) {
                Toast.makeText(this, getString(R.string.error_invalid_file_type), Toast.LENGTH_SHORT).show()
                return
            }
            
            selectedFileUri = uri
            
            // Update UI with file name
            val fileName = UriHelper.getFileNameFromUri(this, uri)
            binding.tvSelectedFile.text = fileName ?: getString(R.string.no_file_selected)
            
            validateInputs()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for file selection", e)
            Toast.makeText(this, getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling file selection", e)
            Toast.makeText(this, getString(R.string.error_file_not_found), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handles directory selection result
     */
    private fun handleDirectorySelection(uri: Uri) {
        try {
            // Take persistable URI permission
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            
            selectedDirectoryUri = uri
            
            // Update UI with directory representation
            val directoryPath = UriHelper.getPathFromTreeUri(this, uri)
            binding.tvSelectedDirectory.text = directoryPath ?: getString(R.string.no_directory_selected)
            
            validateInputs()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for directory selection", e)
            Toast.makeText(this, getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling directory selection", e)
            Toast.makeText(this, getString(R.string.error_no_directory), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Validates all inputs and enables/disables the Process button
     */
    private fun validateInputs() {
        val hasFile = selectedFileUri != null
        val hasDirectory = selectedDirectoryUri != null
        val hasStartRow = binding.etStartRow.text.toString().trim().isNotEmpty()
        val hasEndRow = binding.etEndRow.text.toString().trim().isNotEmpty()
        
        binding.btnProcess.isEnabled = hasFile && hasDirectory && hasStartRow && hasEndRow
    }
    
    /**
     * Processes the Excel file using the existing business logic
     */
    private fun processExcelFile() {
        // Disable button to prevent double-clicks
        binding.btnProcess.isEnabled = false
        
        try {
            // Extract and validate row range
            val startRow = binding.etStartRow.text.toString().trim().toIntOrNull()
            val endRow = binding.etEndRow.text.toString().trim().toIntOrNull()
            
            if (startRow == null || endRow == null) {
                Toast.makeText(this, getString(R.string.error_empty_row_fields), Toast.LENGTH_SHORT).show()
                binding.btnProcess.isEnabled = true
                return
            }
            
            if (startRow < 1 || startRow > endRow) {
                Toast.makeText(this, getString(R.string.error_invalid_row_range), Toast.LENGTH_SHORT).show()
                binding.btnProcess.isEnabled = true
                return
            }
            
            // Validate file URI
            if (selectedFileUri == null || !UriHelper.validateExcelFile(this, selectedFileUri!!)) {
                Toast.makeText(this, getString(R.string.error_no_file), Toast.LENGTH_SHORT).show()
                binding.btnProcess.isEnabled = true
                return
            }
            
            // Launch background processing
            lifecycleScope.launch {
                processExcelFileAsync(startRow, endRow)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in processExcelFile", e)
            Toast.makeText(this, getString(R.string.error_parsing_failed), Toast.LENGTH_SHORT).show()
            binding.btnProcess.isEnabled = true
        }
    }
    
    /**
     * Processes Excel file asynchronously
     */
    private suspend fun processExcelFileAsync(startRow: Int, endRow: Int) {
        var tempFile: File? = null
        var progressDialog: AlertDialog? = null
        
        try {
            // Show progress dialog
            progressDialog = MaterialAlertDialogBuilder(this)
                .setTitle("Processing")
                .setMessage("Processing Excel file...")
                .setCancelable(false)
                .create()
            progressDialog?.show()
            
            val processedData = withContext(Dispatchers.IO) {
                // Copy URI to temp file
                tempFile = UriHelper.copyUriToTempFile(this@MainActivity, selectedFileUri!!)
                if (tempFile == null) {
                    throw Exception("Failed to copy file to temporary location")
                }
                
                // Parse Excel file
                val excelRows = try {
                    ExcelParser().parseExcelFile(tempFile!!.absolutePath, startRow, endRow)
                } catch (e: FileNotFoundException) {
                    throw Exception(getString(R.string.error_file_not_found))
                } catch (e: InvalidRowRangeException) {
                    throw Exception(getString(R.string.error_invalid_row_range))
                } catch (e: ExcelParsingException) {
                    throw Exception(getString(R.string.error_parsing_failed))
                }
                
                // Process data
                DataProcessor().processData(excelRows)
            }
            
            // Switch back to main thread
            withContext(Dispatchers.Main) {
                progressDialog?.dismiss()
                
                // Launch PreviewActivity using activity result launcher
                val intent = Intent(this@MainActivity, PreviewActivity::class.java).apply {
                    putExtra("PROCESSED_DATA", processedData)
                    putExtra("OUTPUT_DIRECTORY_URI", selectedDirectoryUri.toString())
                }
                previewActivityLauncher.launch(intent)
                
                binding.btnProcess.isEnabled = true
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Excel file", e)
            withContext(Dispatchers.Main) {
                progressDialog?.dismiss()
                Toast.makeText(this@MainActivity, e.message ?: getString(R.string.error_parsing_failed), Toast.LENGTH_LONG).show()
                binding.btnProcess.isEnabled = true
            }
        } finally {
            // Clean up temp file
            tempFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }
    
    /**
     * Resets the app state to initial values
     */
    private fun resetAppState() {
        // Clear selected URIs
        selectedFileUri = null
        selectedDirectoryUri = null
        
        // Reset UI text views
        binding.tvSelectedFile.text = getString(R.string.no_file_selected)
        binding.tvSelectedDirectory.text = getString(R.string.no_directory_selected)
        
        // Clear row range fields
        binding.etStartRow.setText("")
        binding.etEndRow.setText("")
        
        // Update button state
        validateInputs()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up any remaining temp files in cache directory
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") && file.name.endsWith(".wav")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp files", e)
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}
