package com.docvault.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility to handle camera file creation and URIs.
 */
class CameraHelper(private val context: Context) {

    /**
     * Creates a temporary file for the camera to save the photo into.
     */
    fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.cacheDir // Store in cache, we will delete after processing
        return File.createTempFile("SCAN_${timeStamp}_", ".jpg", storageDir)
    }

    /**
     * Gets a secure content:// Uri for a file so the camera app can write to it.
     */
    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
