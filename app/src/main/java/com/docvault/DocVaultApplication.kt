// File: app/src/main/java/com/docvault/DocVaultApplication.kt
// STATUS: REPLACE the entire file

package com.docvault

import android.app.Application
import com.docvault.data.database.AppDatabase
import com.docvault.data.repository.DocumentRepository
import com.docvault.security.AutoLockManager
import com.docvault.security.BiometricHelper
import com.docvault.security.EncryptionManager
import com.docvault.security.PinManager

/**
 * The very first thing that runs when the app starts.
 * Sets up all the tools that the rest of the app needs.
 *
 * "lateinit" means: "I promise to set this up before anyone uses it."
 * "by lazy" means: "Don't create this until someone actually asks for it."
 */
class DocVaultApplication : Application() {

    // ── Security tools (created immediately on app start) ───
    lateinit var pinManager: PinManager
    lateinit var biometricHelper: BiometricHelper
    lateinit var encryptionManager: EncryptionManager
    lateinit var autoLockManager: AutoLockManager

    // ── Database (created only when first needed) ───────────
    // "by lazy" = don't create the database until some screen
    // actually tries to read/write documents.
    // This makes the app start faster.
    val database: AppDatabase by lazy {
        val passphrase = encryptionManager.getDatabasePassphrase(this)
        AppDatabase.getInstance(this, passphrase)
    }

    // ── Repository (the "front desk" for all database operations) ──
    val repository: DocumentRepository by lazy {
        DocumentRepository(database.documentDao())
    }

    override fun onCreate() {
        super.onCreate()

        // Save a reference so any part of the app can access these tools
        instance = this

        // Initialize security (order matters!)
        // 1. Encryption first (creates/loads the master key)
        encryptionManager = EncryptionManager()

        // 2. PIN manager (handles PIN storage and verification)
        pinManager = PinManager(this)

        // 3. Biometric helper (fingerprint / face unlock)
        biometricHelper = BiometricHelper(this)

        // 4. Auto-lock (tracks when to lock the app)
        autoLockManager = AutoLockManager(pinManager)
    }

    companion object {
        /**
         * Access the application from anywhere in the app.
         *
         * Usage: DocVaultApplication.instance.pinManager
         *
         * "lateinit" = it will be set in onCreate before anything else runs
         */
        lateinit var instance: DocVaultApplication
            private set
    }
}