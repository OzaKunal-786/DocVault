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
 * Optimized Scanner that only looks for document types (PDF, DOC, etc.)
 * and ignores media files like images and videos.
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
     * Scans the system MediaStore for documents in default folders.
     * Default folders: Download, Documents, WhatsApp Documents.
     */
    suspend fun scanForDocuments(): List<ScannedFile> = withContext(Dispatchers.IO) {
        val foundFiles = mutableListOf<ScannedFile>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri("external")
        } else {
            MediaStore.Files.getContentUri("external")
        }
        
        foundFiles.addAll(queryMediaStore(collection))

        // Deduplicate and filter for relevant folders and types
        foundFiles.distinctBy { it.hash }.filter { isValidDocument(it) }
    }

    /**
     * Recursively scans folders selected via SAF.
     */
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
                if (!file.name?.startsWith(".")!!) traverseFolder(file, results)
            } else if (isDocumentMimeType(file.type)) {
                val scanned = ScannedFile(
                    uri = file.uri,
                    name = file.name ?: "Unknown",
                    path = folder.name ?: "",
                    size = file.length(),
                    mimeType = file.type ?: "",
                    dateModified = file.lastModified(),
                    hash = generateHash(file.uri.toString(), file.length(), file.lastModified())
                )
                results.add(scanned)
            }
        }
    }

    private fun isDocumentMimeType(mimeType: String?): Boolean {
        return mimeType == "application/pdf" || 
               mimeType == "application/msword" || 
               mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
               mimeType == "text/plain" ||
               mimeType == "application/rtf"
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

        // Only query for document MIME types
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?, ?)"
        val args = arrayOf(
            "application/pdf", 
            "application/msword", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/rtf"
        )

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
     * Filters for default folders: Download, Document, WhatsApp Document.
     */
    private fun isValidDocument(file: ScannedFile): Boolean {
        val path = file.path.lowercase()
        
        // Target default folders specifically
        val isDefaultFolder = path.contains("download") || 
                             path.contains("documents") || 
                             path.contains("whatsapp documents")
        
        val isTrash = path.contains("cache") || path.contains(".thumbnails") || path.contains("temp")
        
        return isDefaultFolder && !isTrash
    }

    private fun generateHash(path: String, size: Long, date: Long): String {
        return MessageDigest.getInstance("MD5")
            .digest("$path|$size|$date".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
