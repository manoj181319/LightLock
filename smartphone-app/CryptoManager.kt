package com.lightlock.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.MasterKeys
import androidx.security.crypto.EncryptedSharedPreferences
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Crypto manager for LightLock Phase 2
 * Handles AES-256-GCM encryption/decryption and key storage via Android Keystore
 * As specified in PROTOCOL.md for secure credential transmission
 */
class CryptoManager(private val context: Context) {

    companion object {
        private const val KEY_STORE_NAME = "AndroidKeyStore"
        private const val KEY_NAME = "lightlock_master_key"
        private const val PREFS_NAME = "lightlock_secure_prefs"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    init {
        try {
            generateKeyIfNeeded()
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize CryptoManager", e)
        }
    }

    /**
     * Generates a master key in Android Keystore if it doesn't exist
     */
    private fun generateKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME).apply { load(null) }
        if (!keyStore.containsAlias(KEY_NAME)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_NAME
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_NAME,
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
     * Gets the master key from Android Keystore
     */
    private fun getMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME).apply { load(null) }
        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }

    /**
     * Encrypts data using AES-256-GCM
     * 
     * @param plaintext The data to encrypt
     * @return Encrypted data with IV prepended (IV + ciphertext)
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val key = getMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        // Generate random IV
        val iv = ByteArray(GCM_IV_LENGTH)
        Random().nextBytes(iv)
        
        // Initialize cipher for encryption
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        
        // Encrypt
        val ciphertext = cipher.doFinal(plaintext)
        
        // Return IV + ciphertext
        val output = ByteArrayOutputStream()
        output.write(iv)
        output.write(ciphertext)
        return output.toByteArray()
    }

    /**
     * Decrypts data using AES-256-GCM
     * 
     * @param encryptedData Encrypted data with IV prepended (IV + ciphertext)
     * @return Decrypted plaintext
     * @throws Exception if decryption fails
     */
    fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Encrypted data too short")
        }
        
        val key = getMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        // Extract IV and ciphertext
        val iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH)
        val ciphertext = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.size)
        
        // Initialize cipher for decryption
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
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
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val encryptedBytes = encrypt(plaintextBytes)
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    /**
     * Decrypts a string using AES-256-GCM
     * 
     * @param encryptedData Base64-encoded encrypted data with IV prepended
     * @return Decrypted plaintext string
     * @throws Exception if decryption fails
     */
    fun decryptString(encryptedData: String): String {
        val encryptedBytes = Base64.getDecoder().decode(encryptedData)
        val decryptedBytes = decrypt(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Generates a random nonce as specified in PROTOCOL.md
     * 
     * @param length Length of nonce in bytes (default 16 bytes for challenge)
     * @return Random nonce bytes
     */
    fun generateNonce(length: Int = 16): ByteArray {
        val nonce = ByteArray(length)
        Random().nextBytes(nonce)
        return nonce
    }

    /**
     * Converts nonce to hex string for display/storage
     * 
     * @param nonce The nonce bytes
     * @return Hex string representation
     */
    fun nonceToHex(nonce: ByteArray): String {
        return nonce.map { "%02X".format(it) }.join("")
    }

    /**
     * Converts hex string to nonce bytes
     * 
     * @param hex Hex string representation
     * @return Nonce bytes
     */
    fun hexToNonce(hex: String): ByteArray {
        val bytes = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            val byte = Integer.parseInt(hex.substring(i, i + 2), 16)
            bytes[i / 2] = byte.toByte()
        }
        return bytes
    }
}