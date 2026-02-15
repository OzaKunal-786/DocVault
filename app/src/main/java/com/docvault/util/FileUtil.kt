// Location: app/src/main/java/com/docvault/util/FileUtil.kt

package com.docvault.util

import android.content.Context
import java.io.File
import java.text.DecimalFormat

/**
 * File and storage utility functions.
 */
object FileUtil {

    /**
     * Get the vault directory where encrypted files are stored.
     * Creates the directory if it doesn't exist.
     */
    fun getVaultDir(context: Context): File {
        val dir = File(context.filesDir, "vault/documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get the thumbnails directory.
     */
    fun getThumbnailDir(context: Context): File {
        val dir = File(context.filesDir, "vault/thumbnails")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get a temporary directory for decrypted files.
     * Files here should be deleted after use.
     */
    fun getTempDir(context: Context): File {
        val dir = File(context.cacheDir, "vault_temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Delete all temporary decrypted files.
     * Call this when app goes to background.
     */
    fun clearTempFiles(context: Context) {
        val tempDir = getTempDir(context)
        tempDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Format file size to human readable string.
     * 1024 → "1.0 KB", 1048576 → "1.0 MB"
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatter = DecimalFormat("#,##0.#")
        return "${formatter.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    /**
     * Get file extension from path.
     */
    fun getExtension(path: String): String {
        return path.substringAfterLast('.', "").lowercase()
    }

    /**
     * Check if a file is an image based on extension.
     */
    fun isImage(path: String): Boolean {
        return getExtension(path) in listOf("jpg", "jpeg", "png", "heic", "webp", "bmp")
    }

    /**
     * Check if a file is a PDF.
     */
    fun isPdf(path: String): Boolean {
        return getExtension(path) == "pdf"
    }

    /**
     * Determine source folder name from full path.
     * "/storage/emulated/0/Download/file.pdf" → "Downloads"
     * "/storage/emulated/0/DCIM/Camera/photo.jpg" → "Camera"
     */
    fun getSourceFolder(path: String): String {
        val lowerPath = path.lowercase()
        return when {
            lowerPath.contains("/download") -> "Downloads"
            lowerPath.contains("/dcim/camera") -> "Camera"
            lowerPath.contains("/dcim") -> "Camera"
            lowerPath.contains("/whatsapp") -> "WhatsApp"
            lowerPath.contains("/telegram") -> "Telegram"
            lowerPath.contains("/documents") -> "Documents"
            lowerPath.contains("/pictures") -> "Pictures"
            lowerPath.contains("/screenshots") -> "Screenshots"
            else -> "Other"
        }
    }
}