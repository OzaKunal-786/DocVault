// Location: app/src/main/java/com/docvault/util/HashUtil.kt

package com.docvault.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Utility for computing file hashes.
 * Used for duplicate detection — two files with the same MD5 are duplicates.
 */
object HashUtil {

    /**
     * Compute MD5 hash of a file.
     * Reads file in chunks to handle large files without OOM.
     *
     * @param file The file to hash
     * @return Hex string of MD5 hash (32 characters)
     */
    fun md5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)  // 8 KB chunks

        FileInputStream(file).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute MD5 hash of a byte array.
     * Used for smaller data (thumbnails, etc.)
     */
    fun md5(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
            .digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}