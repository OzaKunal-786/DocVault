package com.docvault.util

import android.content.Context
import android.net.Uri
import com.docvault.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handles high-compression backup and restoration of the entire encrypted vault.
 */
class BackupManager(private val context: Context) {

    /**
     * Creates an extremely compressed ZIP of the encrypted vault.
     * Uses Level 9 compression for smallest possible file size.
     */
    suspend fun createBackup(outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault")
            val dbFile = context.getDatabasePath("docvault_encrypted.db")
            
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // Set maximum compression level (9)
                    zipOut.setLevel(9)
                    
                    // 1. Backup all files in vault directory
                    if (vaultDir.exists()) {
                        vaultDir.walkTopDown().filter { it.isFile }.forEach { file ->
                            val entry = ZipEntry("vault/${file.relativeTo(context.filesDir).path.substringAfter("vault/")}")
                            zipOut.putNextEntry(entry)
                            file.inputStream().use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }
                    
                    // 2. Backup the Database
                    if (dbFile.exists()) {
                        val dbEntry = ZipEntry("database/docvault_encrypted.db")
                        zipOut.putNextEntry(dbEntry)
                        dbFile.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Restores the vault from a backup ZIP by overwriting local data.
     */
    suspend fun restoreBackup(backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Close current database to allow overwriting
            AppDatabase.closeDatabase()
            
            val dbDir = context.getDatabasePath("docvault_encrypted.db").parentFile ?: return@withContext false
            
            context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val destinationFile = if (entry.name.startsWith("vault/")) {
                            File(context.filesDir, entry.name)
                        } else if (entry.name.startsWith("database/")) {
                            File(dbDir, entry.name.substringAfter("database/"))
                        } else {
                            null
                        }

                        destinationFile?.let {
                            it.parentFile?.mkdirs()
                            it.outputStream().use { out -> zipIn.copyTo(out) }
                        }
                        
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
