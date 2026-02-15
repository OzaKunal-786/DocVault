// Location: app/src/main/java/com/docvault/security/BiometricHelper.kt

package com.docvault.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Handles biometric authentication (fingerprint / face).
 * Wraps AndroidX Biometric library.
 */
class BiometricHelper(private val context: Context) {

    /**
     * Check if the device supports biometric authentication and has fingerprints enrolled.
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Check if hardware exists but no biometrics are enrolled.
     * This is useful for prompting the user to set up biometrics.
     */
    fun isHardwareAvailableButNotEnrolled(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    /**
     * Show biometric prompt to user.
     *
     * @param activity The current FragmentActivity
     * @param onSuccess Called when authentication succeeds
     * @param onError Called when authentication fails or is cancelled
     */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onError("USE_PIN") // User chose to use PIN
                    }
                    BiometricPrompt.ERROR_LOCKOUT -> {
                        onError("Too many attempts. Try again later.")
                    }
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onError("Biometrics disabled. Use PIN.")
                    }
                    else -> {
                        onError(errString.toString())
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // System shows its own message. No need to call onError here.
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock DocVault")
            .setSubtitle("Authenticate to access your secure documents")
            .setNegativeButtonText("Use PIN instead")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false) // For face unlock, don't require a confirmation press
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
