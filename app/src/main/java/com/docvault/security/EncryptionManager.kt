package com.docvault.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles AES-256-GCM encryption and decryption.
 */
class EncryptionManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "docvault_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    init {
        if (!keyExists()) {
            generateKey()
        }
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        val key = getKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                output.write(iv)
                val inputBytes = input.readBytes()
                val encryptedBytes = cipher.doFinal(inputBytes)
                output.write(encryptedBytes)
            }
        }
    }

    fun decryptFile(encryptedFile: File, outputFile: File) {
        val key = getKey()
        FileInputStream(encryptedFile).use { input ->
            val iv = ByteArray(GCM_IV_LENGTH)
            input.read(iv)
            val encryptedBytes = input.readBytes()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            FileOutputStream(outputFile).use { output ->
                output.write(decryptedBytes)
            }
        }
    }

    fun encryptBytes(data: ByteArray): ByteArray {
        val key = getKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        return iv + encryptedData
    }

    fun decryptBytes(encryptedData: ByteArray): ByteArray {
        val key = getKey()
        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val data = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(data)
    }

    fun getDatabasePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences("docvault_db_key", Context.MODE_PRIVATE)
        val storedEncryptedPassphrase = prefs.getString("db_passphrase", null)

        if (storedEncryptedPassphrase != null) {
            val encryptedBytes = Base64.decode(storedEncryptedPassphrase, Base64.DEFAULT)
            return decryptBytes(encryptedBytes)
        } else {
            val newPassphrase = ByteArray(32)
            SecureRandom().nextBytes(newPassphrase)
            val encryptedPassphrase = encryptBytes(newPassphrase)
            val encoded = Base64.encodeToString(encryptedPassphrase, Base64.DEFAULT)
            prefs.edit().putString("db_passphrase", encoded).apply()
            return newPassphrase
        }
    }

    private fun keyExists(): Boolean {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.containsAlias(KEYSTORE_ALIAS)
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }
}
