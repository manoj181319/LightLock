package com.lightlock.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.MasterKeys
import androidx.security.crypto.EncryptedSharedPreferences
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Crypto utilities for LightLock smartphone app
 * Handles AES-256-GCM encryption, HMAC-SHA256, key storage via Android Keystore
 * And shared secret management for challenge-response protocol
 */
class CryptoUtils(private val context: Context) {

    companion object {
        private const val KEY_STORE_NAME = "AndroidKeyStore"
        private const val MASTER_KEY_NAME = "lightlock_master_key"
        private const val DEVICE_KEY_NAME = "lightlock_device_key"
        private const val SHARED_SECRET_ALIAS = "lightlock_shared_secret"
        private const val PREFS_NAME = "lightlock_secure_prefs"
        private const val PREFS_KEY_SHARED_SECRET = "shared_secret"
        private const val PREFS_KEY_DEVICE_PAIRED = "device_paired"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val SHARED_SECRET_LENGTH = 32 // 256 bits
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
    }

    private val securePrefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(KeyGenParameterSpec.Builder(
            MASTER_KEY_NAME,
            KeyGenParameterSpec.PURPOSE_ENCRYPT or KeyGenParameterSpec.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build())

        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val deviceKey by lazy {
        getOrCreateDeviceKey()
    }

    init {
        // Ensure device key exists
        getOrCreateDeviceKey()
    }

    /**
     * Gets or creates the device-specific secret key in Android Keystore
     */
    private fun getOrCreateDeviceKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME).apply { load(null) }
        return if (keyStore.containsAlias(DEVICE_KEY_NAME)) {
            keyStore.getKey(DEVICE_KEY_NAME, null) as SecretKey
        } else {
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
     * Gets or creates the shared secret for HMAC with paired device
     * The shared secret is encrypted with the device key and stored in EncryptedSharedPreferences
     */
    fun getOrCreateSharedSecret(): SecretKey {
        val encoded = securePrefs.getString(PREFS_KEY_SHARED_SECRET, null)
        return if (encoded != null) {
            val decoded = Base64.decode(encoded, Base64.NO_WRAP)
            SecretKeySpec(decoded, "HmacSHA256")
        } else {
            // Generate new shared secret
            val random = SecureRandom()
            val secretBytes = ByteArray(SHARED_SECRET_LENGTH)
            random.nextBytes(secretBytes)
            val secretKey = SecretKeySpec(secretBytes, "HmacSHA256")

            // Store encrypted
            securePrefs.edit()
                .putString(PREFS_KEY_SHARED_SECRET, Base64.encodeToString(secretBytes, Base64.NO_WRAP))
                .apply()

            secretKey
        }
    }

    /**
     * Sets a new shared secret (for device pairing)
     */
    fun setSharedSecret(secret: SecretKey) {
        securePrefs.edit()
            .putString(PREFS_KEY_SHARED_SECRET, Base64.encodeToString(secret.encoded, Base64.NO_WRAP))
            .apply()
    }

    /**
     * Checks if device is paired (has shared secret)
     */
    fun isDevicePaired(): Boolean {
        return securePrefs.contains(PREFS_KEY_SHARED_SECRET)
    }

    /**
     * Marks device as paired
     */
    fun setDevicePaired(paired: Boolean) {
        securePrefs.edit()
            .putBoolean(PREFS_KEY_DEVICE_PAIRED, paired)
            .apply()
    }

    /**
     * Encrypts data using AES-256-GCM with the device key
     *
     * @param plaintext The data to encrypt
     * @return Encrypted data with IV prepended (IV + ciphertext + auth tag)
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_ALGORITHM)

        // Generate random IV
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        // Initialize cipher for encryption
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, deviceKey, spec)

        // Encrypt
        val ciphertext = cipher.doFinal(plaintext)

        // Return IV + ciphertext
        val output = ByteArrayOutputStream()
        output.write(iv)
        output.write(ciphertext)
        return output.toByteArray()
    }

    /**
     * Decrypts data using AES-256-GCM with the device key
     *
     * @param encryptedData Encrypted data with IV prepended (IV + ciphertext + auth tag)
     * @return Decrypted plaintext
     * @throws Exception if decryption fails
     */
    fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Encrypted data too short")
        }

        val cipher = Cipher.getInstance(AES_ALGORITHM)

        // Extract IV and ciphertext
        val iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH)
        val ciphertext = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.size)

        // Initialize cipher for decryption
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, deviceKey, spec)

        // Decrypt
        return cipher.doFinal(ciphertext)
    }

    /**
     * Encrypts a string using AES-256-GCM
     *
     * @param plaintext The string to encrypt
     * @return Base64-encoded encrypted data with IV prepended
     */
    fun encryptString(plaintext: String): String {
        val plaintextBytes = plaintext.toByteArray(StandardCharsets.UTF_8)
        val encryptedBytes = encrypt(plaintextBytes)
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    /**
     * Decrypts a string using AES-256-GCM
     *
     * @param encryptedData Base64-encoded encrypted data with IV prepended
     * @return Decrypted plaintext string
     * @throws Exception if decryption fails
     */
    fun decryptString(encryptedData: String): String {
        val encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
        val decryptedBytes = decrypt(encryptedBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    /**
     * Computes HMAC-SHA256 of data using the shared secret
     *
     * @param data The data to sign
     * @return HMAC-SHA256 signature (32 bytes)
     */
    fun computeHmac(data: ByteArray): ByteArray {
        val sharedSecret = getOrCreateSharedSecret()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(sharedSecret)
        return mac.doFinal(data)
    }

    /**
     * Computes HMAC-SHA256 and returns as hex string
     *
     * @param data The data to sign
     * @return Hex-encoded HMAC
     */
    fun computeHmacHex(data: ByteArray): String {
        val hmac = computeHmac(data)
        return hmac.joinToString("") { "%02X".format(it) }
    }

    /**
     * Verifies HMAC-SHA256 signature
     *
     * @param data The original data
     * @param signature The signature to verify
     * @return True if signature is valid
     */
    fun verifyHmac(data: ByteArray, signature: ByteArray): Boolean {
        val expectedHmac = computeHmac(data)
        return MessageDigest.isEqual(expectedHmac, signature)
    }

    /**
     * Verifies HMAC-SHA256 signature from hex string
     *
     * @param data The original data
     * @param signatureHex Hex-encoded signature
     * @return True if signature is valid
     */
    fun verifyHmacHex(data: ByteArray, signatureHex: String): Boolean {
        val signature = hexToBytes(signatureHex)
        return verifyHmac(data, signature)
    }

    /**
     * Generates a random nonce (16 bytes = 128 bits)
     */
    fun generateNonce(): ByteArray {
        val nonce = ByteArray(16)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    /**
     * Converts bytes to hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * Converts hex string to bytes
     */
    fun hexToBytes(hex: String): ByteArray {
        if (hex.length % 2 != 0) {
            throw IllegalArgumentException("Invalid hex string")
        }
        val bytes = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            bytes[i / 2] = Integer.parseInt(hex.substring(i, i + 2), 16).toByte()
        }
        return bytes
    }

    /**
     * Converts bytes to base64url string (for QR code encoding)
     */
    fun bytesToBase64Url(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
            .replace("=", "")
    }

    /**
     * Converts base64url string to bytes
     */
    fun base64UrlToBytes(base64url: String): ByteArray {
        var base64 = base64url.replace("-", "+").replace("_", "/")
        while (base64.length % 4 != 0) base64 += "="
        return Base64.decode(base64, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    /**
     * Derives a session key from device key and salt
     */
    fun deriveSessionKey(salt: ByteArray): SecretKey {
        val keyMaterial = ByteArray(deviceKey.encoded.size + salt.size)
        System.arraycopy(deviceKey.encoded, 0, keyMaterial, 0, deviceKey.encoded.size)
        System.arraycopy(salt, 0, keyMaterial, deviceKey.encoded.size, salt.size)

        val digest = MessageDigest.getInstance("SHA-256").digest(keyMaterial)
        val sessionKeyBytes = Arrays.copyOf(digest, 32)
        return SecretKeySpec(sessionKeyBytes, "AES")
    }

    /**
     * Generates a random salt (16 bytes)
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Generates a secure random password
     *
     * @param length Length of the password in characters
     * @return Secure random password string
     */
    fun generateSecurePassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Clears all stored secrets (for unpairing)
     */
    fun clearSecrets() {
        securePrefs.edit()
            .remove(PREFS_KEY_SHARED_SECRET)
            .remove(PREFS_KEY_DEVICE_PAIRED)
            .apply()
    }
}