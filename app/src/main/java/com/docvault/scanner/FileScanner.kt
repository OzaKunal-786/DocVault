package com.docvault.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.docvault.data.database.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Scans the device storage for document-like files (Images, PDFs).
 * Filters out small files (memes/stickers) and non-document folders.
 */
class FileScanner(private val context: Context) {

    data class ScannedFile(
        val uri: Uri,
        val name: String,
        val path: String,
        val size: Long,
        val mimeType: String,
        val dateModified: Long,
        val hash: String
    )

    /**
     * Main scanning function. Queries MediaStore for images and PDFs.
     */
    suspend fun scanForDocuments(): List<ScannedFile> = withContext(Dispatchers.IO) {
        val foundFiles = mutableListOf<ScannedFile>()
        
        // 1. Scan Images
        foundFiles.addAll(queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
        
        // 2. Scan Documents (PDFs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            foundFiles.addAll(queryMediaStore(MediaStore.Files.getContentUri("external")))
        } else {
            // Older versions use a slightly different approach for PDFs
            foundFiles.addAll(queryMediaStore(MediaStore.Files.getContentUri("external")))
        }

        // 3. Filter and Deduplicate
        foundFiles.distinctBy { it.hash }
            .filter { isValidDocument(it) }
    }

    private fun queryMediaStore(collection: Uri): List<ScannedFile> {
        val files = mutableListOf<ScannedFile>()
        
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        // Filter for images and PDFs only
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR " +
                        "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR " +
                        "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR " +
                        "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        
        val selectionArgs = arrayOf(
            "image/jpeg",
            "image/png",
            "application/pdf",
            "image/webp"
        )

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val path = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeColumn) ?: ""
                val dateModified = cursor.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)

                // Generate a quick hash for the file (using path + size + date as a proxy for MD5 if file is large)
                // In a real production app, we'd do a partial MD5 for speed.
                val hash = generateHash(path, size, dateModified)

                files.add(ScannedFile(contentUri, name, path, size, mimeType, dateModified, hash))
            }
        }
        return files
    }

    /**
     * Filters out files that are likely not documents (too small, or in cache/app folders).
     */
    private fun isValidDocument(file: ScannedFile): Boolean {
        // Skip files smaller than 50KB (likely memes, icons, or stickers)
        if (file.size < 50 * 1024) return false

        val pathLower = file.path.lowercase()
        
        // Only keep files from relevant folders
        val relevantFolders = listOf(
            "download",
            "documents",
            "whatsapp/media/whatsapp documents",
            "whatsapp/media/whatsapp images",
            "dcim/camera",
            "pictures"
        )

        val isInRelevantFolder = relevantFolders.any { pathLower.contains(it) }
        
        // Filter out known "trash" folders
        val isTrash = pathLower.contains("cache") || 
                      pathLower.contains(".thumbnails") || 
                      pathLower.contains("temp") ||
                      pathLower.contains("instagram") ||
                      pathLower.contains("sent") // Skip sent WhatsApp files to avoid duplicates

        return isInRelevantFolder && !isTrash
    }

    private fun generateHash(path: String, size: Long, date: Long): String {
        val raw = "$path|$size|$date"
        return MessageDigest.getInstance("MD5")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
