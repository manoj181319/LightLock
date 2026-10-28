package com.lightlock.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.MasterKeys
import java.security.KeyStore
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Challenge handler for LightLock Phase 2
 * Implements the challenge-response state machine as per PROTOCOL.md
 * Handles secure key management and session key derivation
 */
class ChallengeHandler(private val context: Context) {

    companion object {
        private const val KEY_STORE_NAME = "AndroidKeyStore"
        private const val DEVICE_KEY_NAME = "lightlock_device_key"
        private const val SESSION_SALT_LENGTH = 16
        private const val SESSION_KEY_LENGTH = 32 // 256 bits for AES-256
        private const val NONCE_LENGTH = 16 // 128 bits for challenge nonce
    }

    init {
        try {
            generateDeviceKeyIfNeeded()
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize ChallengeHandler", e)
        }
    }

    /**
     * Generates a device-specific secret key in Android Keystore if it doesn't exist
     * As per PROTOCOL.md: "Generates and stores a device-specific secret key in the Android Keystore on first run"
     */
    private fun generateDeviceKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME).apply { load(null) }
        if (!keyStore.containsAlias(DEVICE_KEY_NAME)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_NAME
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                DEVICE_KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .setUnlockedDeviceRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    /**
     * Gets the device-specific secret key from Android Keystore
     * As per PROTOCOL.md: "Derives per-session encryption keys from the device secret and a salt"
     */
    private fun getDeviceKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME).apply { load(null) }
        return keyStore.getKey(DEVICE_KEY_NAME, null) as SecretKey
    }

    /**
     * Generates a random session salt
     * 
     * @return Random salt bytes
     */
    fun generateSessionSalt(): ByteArray {
        val salt = ByteArray(SESSION_SALT_LENGTH)
        Random().nextBytes(salt)
        return salt
    }

    /**
     * Derives a session key from the device secret and salt
     * As per PROTOCOL.md: "Derives per-session encryption keys from the device secret and a salt"
     * 
     * @param deviceKey The device-specific secret key from Keystore
     * @param salt The session salt
     * @return Derived session key (AES-256)
     */
    fun deriveSessionKey(deviceKey: SecretKey, salt: ByteArray): SecretKey {
        // Simple key derivation using SHA-256 of device key + salt
        // In production, you might want to use a proper KDF like HKDF
        val keyMaterial = ByteArray(deviceKey.encoded.size + salt.size)
        System.arraycopy(deviceKey.encoded, 0, keyMaterial, 0, deviceKey.encoded.size)
        System.arraycopy(salt, 0, keyMaterial, deviceKey.encoded.size, salt.size)
        
        // Hash to get fixed-length key
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(keyMaterial)
        val sessionKeyBytes = Arrays.copyOf(digest, SESSION_KEY_LENGTH)
        
        return SecretKeySpec(sessionKeyBytes, "AES")
    }

    /**
     * Generates a challenge nonce as specified in PROTOCOL.md
     * 
     * @return Challenge nonce bytes (16 bytes as per spec)
     */
    fun generateChallenge(): ByteArray {
        val nonce = ByteArray(NONCE_LENGTH)
        Random().nextBytes(nonce)
        return nonce
    }

    /**
     * Converts challenge nonce to Base64 string for transmission/display
     * 
     * @param nonce The challenge nonce
     * @return Base64-encoded string
     */
    fun challengeToBase64(nonce: ByteArray): String {
        return Base64.encodeToString(nonce, Base64.NO_WRAP)
    }

    /**
     * Converts Base64 string to challenge nonce
     * 
     * @param base64 Base64-encoded challenge
     * @return Challenge nonce bytes
     */
    fun base64ToChallenge(base64: String): ByteArray {
        return Base64.decode(base64, Base64.DEFAULT)
    }

    /**
     * Validates that a received challenge matches the expected challenge
     * As per PROTOCOL.md: "The computer extracts the challenge from the received payload 
     * and compares it with the originally generated nonce. A match, combined with valid CRC-8, 
     * constitutes successful authentication."
     * 
     * @param receivedChallenge The challenge received from the phone
     * @param expectedChallenge The challenge originally generated by the computer
     * @return True if challenges match
     */
    fun validateChallenge(receivedChallenge: ByteArray, expectedChallenge: ByteArray): Boolean {
        if (receivedChallenge.size != expectedChallenge.size) {
            return false
        }
        return MessageDigest.isEqual(receivedChallenge, expectedChallenge)
    }

    /**
     * Creates a secure session identifier for replay protection
     * As per PROTOCOL.md: "The browser extension maintains a rolling window of the last 1000 used nonces"
     * 
     * @param nonce The challenge nonce
     * @param timestamp Optional timestamp for replay window
     * @return Session identifier string
     */
    fun createSessionId(nonce: ByteArray, timestamp: Long = System.currentTimeMillis()): String {
        val nonceBase64 = challengeToBase64(nonce)
        return "$nonceBase64:$timestamp"
    }
}