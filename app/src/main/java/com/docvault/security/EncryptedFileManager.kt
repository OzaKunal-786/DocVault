package com.docvault.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles per-file encryption using AES-256-GCM.
 * Keys are stored in the Android Keystore system.
 */
class EncryptedFileManager(private val context: Context) {

    private val vaultDir = File(context.filesDir, "vault/documents").apply { mkdirs() }
    private val thumbDir = File(context.filesDir, "vault/thumbnails").apply { mkdirs() }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
    }

    /**
     * Encrypts a file and saves it to the vault.
     */
    fun encryptAndStore(sourceFile: File, documentId: String, isThumbnail: Boolean = false): File? {
        try {
            val key = getOrCreateKey(documentId)
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val destinationDir = if (isThumbnail) thumbDir else vaultDir
            val destinationFile = File(destinationDir, "$documentId.vault")

            FileOutputStream(destinationFile).use { fos ->
                // Write the IV (initialization vector) first so we can decrypt later
                fos.write(iv.size)
                fos.write(iv)

                val cipherOutputStream = javax.crypto.CipherOutputStream(fos, cipher)
                FileInputStream(sourceFile).use { fis ->
                    fis.copyTo(cipherOutputStream)
                }
                cipherOutputStream.close()
            }
            return destinationFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Decrypts a file from the vault to a temporary file.
     */
    fun decryptToTemp(documentId: String, isThumbnail: Boolean = false): File? {
        try {
            val sourceDir = if (isThumbnail) thumbDir else vaultDir
            val sourceFile = File(sourceDir, "$documentId.vault")
            if (!sourceFile.exists()) return null

            val key = getOrCreateKey(documentId)
            
            FileInputStream(sourceFile).use { fis ->
                val ivSize = fis.read()
                val iv = ByteArray(ivSize)
                fis.read(iv)

                val cipher = Cipher.getInstance(AES_MODE)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                val tempFile = File(context.cacheDir, "temp_$documentId" + if (isThumbnail) "_thumb" else ".pdf")
                val cipherInputStream = javax.crypto.CipherInputStream(fis, cipher)
                
                FileOutputStream(tempFile).use { fos ->
                    cipherInputStream.copyTo(fos)
                }
                return tempFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
