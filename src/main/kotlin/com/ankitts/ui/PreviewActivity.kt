package com.ankitts.exltotts.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ankitts.exltotts.databinding.ActivityPreviewBinding
import com.ankitts.model.ProcessedData
import com.ankitts.ui.adapter.UnifiedPreviewAdapter

/**
 * Preview activity for displaying processed Excel data.
 * Shows three sections: Regular Words, Irregular Plurals, and Irregular Verbs.
 */
class PreviewActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPreviewBinding
    private lateinit var processedData: ProcessedData
    private lateinit var outputDirectoryUri: String
    
    private lateinit var unifiedAdapter: UnifiedPreviewAdapter
    
    private lateinit var ttsLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Activity Result launcher
        ttsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Pass the result back to MainActivity
                setResult(RESULT_OK, result.data)
                finish()
            } else {
                // Handle cancellation or error
                finish()
            }
        }
        
        try {
            // Extract Intent data
            extractIntentData()
            
            // Early return if activity is finishing (data was missing)
            if (isFinishing) return
            
            // Initialize ViewBinding
            binding = ActivityPreviewBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Setup action bar
            setupActionBar()
            
            // Initialize unified adapter
            initializeUnifiedAdapter()
            
            // Setup RecyclerView
            setupRecyclerView()
            
            // Bind data to adapter
            bindDataToAdapter()
            
            // Setup start button
            setupStartButton()
            
        } catch (e: Exception) {
            Log.e("PreviewActivity", "Error in onCreate", e)
            Toast.makeText(this, getString(com.ankitts.exltotts.R.string.error_no_data), Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun extractIntentData() {
        // Extract ProcessedData using type-safe getParcelableExtra for API 33+
        val processedDataParcelable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PROCESSED_DATA", ProcessedData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<ProcessedData>("PROCESSED_DATA")
        }
        
        if (processedDataParcelable == null) {
            Toast.makeText(this, getString(com.ankitts.exltotts.R.string.error_no_data), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        processedData = processedDataParcelable
        
        // Extract output directory URI
        outputDirectoryUri = intent.getStringExtra("OUTPUT_DIRECTORY_URI") ?: ""
        
        // Validate output directory URI
        if (outputDirectoryUri.isEmpty()) {
            Toast.makeText(this, getString(com.ankitts.exltotts.R.string.error_no_directory), Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }
    
    private fun setupActionBar() {
        supportActionBar?.title = getString(com.ankitts.exltotts.R.string.preview_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun initializeUnifiedAdapter() {
        unifiedAdapter = UnifiedPreviewAdapter()
    }
    
    private fun setupRecyclerView() {
        binding.rvPreviewContent.apply {
            adapter = unifiedAdapter
            layoutManager = LinearLayoutManager(this@PreviewActivity)
            setHasFixedSize(false)
        }
    }
    
    private fun bindDataToAdapter() {
        // Check if all three lists are empty
        val allListsEmpty = processedData.regularWords.isEmpty() && 
                           processedData.irregularPlurals.isEmpty() && 
                           processedData.irregularVerbs.isEmpty()
        
        if (allListsEmpty) {
            // Show empty state message and hide RecyclerView
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvPreviewContent.visibility = View.GONE
            
            // Disable start conversion button
            binding.btnStartConversion.isEnabled = false
            binding.btnStartConversion.text = getString(com.ankitts.exltotts.R.string.start_conversion_disabled)
        } else {
            // Show RecyclerView and hide empty state message
            binding.tvEmptyState.visibility = View.GONE
            binding.rvPreviewContent.visibility = View.VISIBLE
            
            // Enable start conversion button
            binding.btnStartConversion.isEnabled = true
            binding.btnStartConversion.text = getString(com.ankitts.exltotts.R.string.start_conversion_button)
            
            // Bind data to adapter
            unifiedAdapter.submitData(
                this,
                processedData.regularWords,
                processedData.irregularPlurals,
                processedData.irregularVerbs
            )
        }
    }
    
    private fun setupStartButton() {
        binding.btnStartConversion.setOnClickListener {
            val intent = Intent(this, TTSActivity::class.java).apply {
                putExtra("PROCESSED_DATA", processedData)
                putExtra("OUTPUT_DIRECTORY_URI", outputDirectoryUri)
            }
            ttsLauncher.launch(intent)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}