package com.docvault.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Optimized Scanner with strict filters to avoid "junk" files.
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

    suspend fun scanForDocuments(): List<ScannedFile> = withContext(Dispatchers.IO) {
        val foundFiles = mutableListOf<ScannedFile>()
        
        // Use MediaStore for speed
        foundFiles.addAll(queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
        
        val docUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri("external")
        } else {
            MediaStore.Files.getContentUri("external")
        }
        foundFiles.addAll(queryMediaStore(docUri))

        // Deduplicate and strictly filter
        foundFiles.distinctBy { it.hash }.filter { isValidDocument(it) }
    }

    suspend fun scanFolderRecursively(rootUri: Uri): List<ScannedFile> = withContext(Dispatchers.IO) {
        val foundFiles = mutableListOf<ScannedFile>()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
        if (rootDoc != null && rootDoc.isDirectory) {
            traverseFolder(rootDoc, foundFiles)
        }
        foundFiles.distinctBy { it.hash }
    }

    private fun traverseFolder(folder: DocumentFile, results: MutableList<ScannedFile>) {
        folder.listFiles().forEach { file ->
            if (file.isDirectory) {
                // Skip hidden or app-specific directories
                if (!file.name?.startsWith(".")!!) traverseFolder(file, results)
            } else if (isSupportedMimeType(file.type)) {
                val scanned = ScannedFile(
                    uri = file.uri,
                    name = file.name ?: "Unknown",
                    path = folder.name ?: "",
                    size = file.length(),
                    mimeType = file.type ?: "",
                    dateModified = file.lastModified(),
                    hash = generateHash(file.uri.toString(), file.length(), file.lastModified())
                )
                if (isValidDocument(scanned)) {
                    results.add(scanned)
                }
            }
        }
    }

    private fun isSupportedMimeType(mimeType: String?): Boolean {
        return mimeType == "image/jpeg" || mimeType == "image/png" || 
               mimeType == "application/pdf" || mimeType == "image/webp"
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

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?)"
        val args = arrayOf("image/jpeg", "image/png", "application/pdf", "image/webp")

        context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val contentUri = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                val path = cursor.getString(dataCol) ?: ""
                val size = cursor.getLong(sizeCol)
                val date = cursor.getLong(dateCol)
                
                files.add(ScannedFile(
                    uri = contentUri,
                    name = cursor.getString(nameCol) ?: "Unknown",
                    path = path,
                    size = size,
                    mimeType = cursor.getString(mimeCol) ?: "",
                    dateModified = date,
                    hash = generateHash(path, size, date)
                ))
            }
        }
        return files
    }

    /**
     * Strict filtering to ignore junk like WhatsApp Stickers, small icons, etc.
     */
    private fun isValidDocument(file: ScannedFile): Boolean {
        // 1. Minimum size: ignore files < 80KB (stickers/icons)
        if (file.size < 80 * 1024) return false

        val path = file.path.lowercase()
        val name = file.name.lowercase()

        // 2. Ignore known junk folders/files
        val junkTerms = listOf(
            "sticker", "cache", "thumb", "temp", "icon", "emoji", 
            "instagram", "sent", "private", "avatars", ".trashed"
        )
        if (junkTerms.any { path.contains(it) || name.contains(it) }) return false

        // 3. Keep files from relevant locations
        val relevantFolders = listOf("download", "documents", "whatsapp", "dcim", "pictures", "camera")
        return relevantFolders.any { path.contains(it) }
    }

    private fun generateHash(path: String, size: Long, date: Long): String {
        return MessageDigest.getInstance("MD5")
            .digest("$path|$size|$date".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
