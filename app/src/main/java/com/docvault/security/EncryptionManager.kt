// File: app/src/main/java/com/docvault/security/EncryptionManager.kt
// STATUS: REPLACE the entire file with this version

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
 *
 * Simple explanation:
 * - On first launch, creates a secret key inside Android's secure vault
 *   (called "Keystore" — it's hardware-protected on most phones)
 * - Uses that key to encrypt/decrypt every document file
 * - Also creates a separate password for the database encryption
 *
 * Think of it like:
 * - The Keystore key = the master key to your house
 * - Each encrypted file = a locked box inside your house
 * - The database password = the combination to your filing cabinet
 */
class EncryptionManager {

    companion object {
        private const val KEYSTORE_ALIAS = "docvault_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12   // 12 bytes for the "starting point" of encryption
        private const val GCM_TAG_LENGTH = 128  // 128 bits for tamper detection
    }

    init {
        // Generate master key if this is the first time
        if (!keyExists()) {
            generateKey()
        }
    }

    // ═══════════════════════════════════════════════════
    // FILE ENCRYPTION (for documents and thumbnails)
    // ═══════════════════════════════════════════════════

    /**
     * Encrypt a file and save the encrypted version.
     *
     * @param inputFile  The original file (e.g., a PDF)
     * @param outputFile Where to save the encrypted file (e.g., abc123.vault)
     */
    fun encryptFile(inputFile: File, outputFile: File) {
        val key = getKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        // The "IV" is a random starting point — makes each encryption unique
        // even if the same file is encrypted twice
        val iv = cipher.iv

        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                // Write the IV as the first 12 bytes of the file
                // (we need it later to decrypt)
                output.write(iv)

                // Encrypt the actual file content and write it
                val inputBytes = input.readBytes()
                val encryptedBytes = cipher.doFinal(inputBytes)
                output.write(encryptedBytes)
            }
        }
    }

    /**
     * Decrypt a .vault file back to readable content.
     *
     * @param encryptedFile The encrypted .vault file
     * @param outputFile    Where to save the decrypted file (temporary)
     */
    fun decryptFile(encryptedFile: File, outputFile: File) {
        val key = getKey()

        FileInputStream(encryptedFile).use { input ->
            // Read the IV from the first 12 bytes
            val iv = ByteArray(GCM_IV_LENGTH)
            input.read(iv)

            // Read the rest (the encrypted content)
            val encryptedBytes = input.readBytes()

            // Decrypt using the same key + stored IV
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            // Write the decrypted content
            FileOutputStream(outputFile).use { output ->
                output.write(decryptedBytes)
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // BYTES ENCRYPTION (for thumbnails and small data)
    // ═══════════════════════════════════════════════════

    /**
     * Encrypt raw bytes. Returns IV + encrypted data combined.
     */
    fun encryptBytes(data: ByteArray): ByteArray {
        val key = getKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)

        // Combine: first 12 bytes = IV, rest = encrypted content
        return iv + encryptedData
    }

    /**
     * Decrypt raw bytes. Expects IV + encrypted data combined.
     */
    fun decryptBytes(encryptedData: ByteArray): ByteArray {
        val key = getKey()

        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val data = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(data)
    }

    // ═══════════════════════════════════════════════════
    // DATABASE PASSPHRASE (FIXED VERSION)
    // ═══════════════════════════════════════════════════

    /**
     * Get the password used to encrypt the database.
     *
     * How it works:
     * - First time: generates a random 32-byte password,
     *   encrypts it, and saves the encrypted version
     * - Every future time: reads the saved encrypted password,
     *   decrypts it, and returns the SAME password
     *
     * This way the database always opens with the same password.
     *
     * @param context Android context (needed to access storage)
     * @return The database encryption password (always the same)
     */
    fun getDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(
            "docvault_db_key",
            Context.MODE_PRIVATE
        )
        val storedEncryptedPassphrase = prefs.getString("db_passphrase", null)

        if (storedEncryptedPassphrase != null) {
            // We already have a passphrase — decrypt and return it
            val encryptedBytes = Base64.decode(storedEncryptedPassphrase, Base64.DEFAULT)
            return decryptBytes(encryptedBytes)
        } else {
            // First time ever — generate a new random passphrase
            val newPassphrase = ByteArray(32) // 32 bytes = 256 bits
            SecureRandom().nextBytes(newPassphrase)

            // Encrypt it so it's safe on disk
            val encryptedPassphrase = encryptBytes(newPassphrase)

            // Save the encrypted version
            val encoded = Base64.encodeToString(encryptedPassphrase, Base64.DEFAULT)
            prefs.edit().putString("db_passphrase", encoded).apply()

            return newPassphrase
        }
    }

    // ═══════════════════════════════════════════════════
    // INTERNAL KEY MANAGEMENT
    // ═══════════════════════════════════════════════════

    /**
     * Check if we already have a master key in the Keystore.
     */
    private fun keyExists(): Boolean {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.containsAlias(KEYSTORE_ALIAS)
    }

    /**
     * Generate a new master key in Android Keystore.
     * This only happens ONCE — the very first time the app launches.
     */
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }

    /**
     * Retrieve the master key from Android Keystore.
     */
    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }
}