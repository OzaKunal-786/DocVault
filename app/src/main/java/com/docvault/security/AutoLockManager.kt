// Location: app/src/main/java/com/docvault/security/AutoLockManager.kt

package com.docvault.security

/**
 * Tracks when app goes to background and determines
 * if the app should be locked when returning.
 *
 * Usage:
 *   - Call onAppBackgrounded() when app goes to background
 *   - Call shouldLock() when app comes to foreground
 *   - If shouldLock() returns true → show lock screen
 */
class AutoLockManager(private val pinManager: PinManager) {

    private var backgroundTimestamp: Long = 0L
    private var isLocked: Boolean = true  // locked on first launch

    /**
     * Call this when the app goes to background
     * (from Activity.onStop or Lifecycle observer)
     */
    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    /**
     * Check if the app should show the lock screen.
     * Returns true if enough time has passed since going to background.
     */
    fun shouldLock(): Boolean {
        if (isLocked) return true
        if (backgroundTimestamp == 0L) return true

        val elapsed = System.currentTimeMillis() - backgroundTimestamp
        val timeoutMs = pinManager.getAutoLockSeconds() * 1000L

        return elapsed > timeoutMs
    }

    /**
     * Call this after successful authentication (PIN or biometric)
     */
    fun onUnlocked() {
        isLocked = false
        backgroundTimestamp = 0L
    }

    /**
     * Force lock (e.g., user taps "Lock now" button)
     */
    fun lockNow() {
        isLocked = true
    }
}