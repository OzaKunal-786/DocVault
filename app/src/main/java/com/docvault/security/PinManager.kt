package com.docvault.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest

/**
 * Manages PIN creation, storage, and verification.
 *
 * Simple explanation:
 * - When user creates a PIN, we "scramble" it (called hashing)
 *   so even if someone reads the storage, they can't see the real PIN.
 * - The scrambled PIN is stored in an encrypted preferences file.
 * - When user enters PIN again, we scramble their input the same way
 *   and check if the scrambled versions match.
 */
class PinManager(context: Context) {

    // MasterKeys creates a key to encrypt the preferences file
    // Think of it as: the key to the safe where we keep the PIN
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    // EncryptedSharedPreferences = a small encrypted storage file
    // Perfect for storing settings, PINs, preferences
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "docvault_secure_prefs",           // file name
        masterKeyAlias,                     // encryption key
        context,                            // app context
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SET = "pin_is_set"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_SECONDS = "auto_lock_seconds"
    }

    /**
     * Check if user has created a PIN (first launch check)
     */
    fun isPinSet(): Boolean {
        return prefs.getBoolean(KEY_PIN_SET, false)
    }

    /**
     * Create a new PIN. Scrambles (hashes) it before storing.
     * @param pin The raw PIN string (4-6 digits)
     */
    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_PIN_SET, true)
            .apply()
    }

    /**
     * Verify a PIN against the stored hash.
     * @param pin The raw PIN to verify
     * @return true if PIN matches
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    /**
     * Change PIN. Requires old PIN verification first.
     * @return true if old PIN was correct and new PIN is saved
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        setPin(newPin)
        return true
    }

    /**
     * Get the PIN hash — used as part of database encryption key.
     * Returns null if PIN is not set.
     */
    fun getPinHash(): String? {
        return prefs.getString(KEY_PIN_HASH, null)
    }

    // ── Biometric preference ────────────────────────────

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    // ── Auto-lock timeout ───────────────────────────────

    fun getAutoLockSeconds(): Int {
        return prefs.getInt(KEY_AUTO_LOCK_SECONDS, 30)
    }

    fun setAutoLockSeconds(seconds: Int) {
        prefs.edit()
            .putInt(KEY_AUTO_LOCK_SECONDS, seconds)
            .apply()
    }

    // ── Internal ────────────────────────────────────────

    /**
     * Scramble a PIN using SHA-256.
     * "1234" becomes "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"
     * This is a one-way process — you can't un-scramble it.
     */
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}