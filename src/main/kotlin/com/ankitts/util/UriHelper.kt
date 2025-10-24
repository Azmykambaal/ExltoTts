package com.ankitts.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Utility class for handling Android URI operations
 */
object UriHelper {

    /**
     * Extract the display name from a content URI
     * @param context Android context
     * @param uri Content URI
     * @return File name or null if extraction fails
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        it.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copy content from a URI to a temporary file
     * @param context Android context
     * @param uri Content URI
     * @return Temporary file or null on failure
     */
    fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val tempFile = File(context.cacheDir, "temp_${UUID.randomUUID()}.xlsx")
                val outputStream = FileOutputStream(tempFile)
                outputStream.use { output ->
                    input.copyTo(output)
                }
                tempFile
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert a document tree URI to a usable path string
     * @param context Android context
     * @param treeUri Document tree URI
     * @return URI string representation or null on failure
     */
    fun getPathFromTreeUri(context: Context, treeUri: Uri): String? {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, treeUri)
            documentFile?.uri?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validate that the selected URI points to an Excel file
     * @param context Android context
     * @param uri Content URI
     * @return true if valid Excel file, false otherwise
     */
    fun validateExcelFile(context: Context, uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType != null) {
                mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                        mimeType == "application/vnd.ms-excel"
            } else {
                // Fall back to file extension check when MIME type is null
                val fileName = getFileNameFromUri(context, uri)
                fileName?.let { name ->
                    name.lowercase().endsWith(".xlsx") || name.lowercase().endsWith(".xls")
                } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }
}
