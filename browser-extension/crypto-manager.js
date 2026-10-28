/**
 * Crypto manager for LightLock Phase 2
 * Handles AES-256-GCM operations using the Web Crypto API
 * As specified in PROTOCOL.md for secure credential transmission
 * Added HMAC-SHA256 support for challenge-response verification
 */

class CryptoManager {
    // Constants
    static get GCM_IV_LENGTH() { return 12; } // 96 bits for GCM
    static get GCM_TAG_LENGTH() { return 16; } // 128 bits
    static get AES_KEY_LENGTH() { return 256; } // 256-bit key
    static get ALGORITHM() { return {name: "AES-GCM", ivLength: CryptoManager.GCM_IV_LENGTH * 8}; }
    static get HMAC_ALGORITHM() { return {name: "HMAC", hash: "SHA-256"}; }

    constructor() {
        // Note: Key storage would typically use IndexedDB or the Credential Management API
        // For this implementation, we'll assume the key is provided or derived
        // In a production app, you'd use the Web Crypto API to generate/store keys
    }

    /**
     * Generates a random key for AES-GCM
     * @returns Promise{CryptoKey}
     */
    static async generateKey() {
        return await crypto.subtle.generateKey(
            CryptoManager.ALGORITHM,
            true, // extractable
            ["encrypt", "decrypt"]
        );
    }

    /**
     * Imports a raw key for AES-GCM
     * @param keyData The raw key bytes
     * @returns Promise{CryptoKey}
     */
    static async importKey(keyData) {
        return await crypto.subtle.importKey(
            "raw",
            keyData,
            CryptoManager.ALGORITHM,
            true, // extractable
            ["encrypt", "decrypt"]
        );
    }

    /**
     * Imports a raw key for HMAC
     * @param keyData The raw key bytes
     * @returns Promise{CryptoKey}
     */
    static async importHmacKey(keyData) {
        return await crypto.subtle.importKey(
            "raw",
            keyData,
            CryptoManager.HMAC_ALGORITHM,
            false, // not extractable for signing keys
            ["sign", "verify"]
        );
    }

    /**
     * Generates a random key for HMAC
     * @returns Promise{CryptoKey}
     */
    static async generateHmacKey() {
        return await crypto.subtle.generateKey(
            CryptoManager.HMAC_ALGORITHM,
            true,
            ["sign", "verify"]
        );
    }

    /**
     * Exports a key as raw bytes
     * @param key The CryptoKey to export
     * @returns Promise<ArrayBuffer>
     */
    static async exportKey(key) {
        return await crypto.subtle.exportKey("raw", key);
    }

    /**
     * Encrypts data using AES-GCM
     *
     * @param key The CryptoKey to use for encryption
     * @param plaintext The data to encrypt (as ArrayBuffer or string)
     * @returns Promise<ArrayBuffer> containing IV + ciphertext
     */
    static async encrypt(key, plaintext) {
        // Convert string to ArrayBuffer if needed
        if (typeof plaintext === 'string') {
            const encoder = new TextEncoder();
            plaintext = encoder.encode(plaintext);
        } else if (!(plaintext instanceof ArrayBuffer)) {
            plaintext = new Uint8Array(plaintext).buffer;
        }

        // Generate random IV
        const iv = crypto.getRandomValues(new Uint8Array(CryptoManager.GCM_IV_LENGTH));

        // Encrypt
        const ciphertextBuffer = await crypto.subtle.encrypt(
            {name: "AES-GCM", iv: iv},
            key,
            plaintext
        );

        // Combine IV + ciphertext
        const result = new Uint8Array(CryptoManager.GCM_IV_LENGTH + ciphertextBuffer.byteLength);
        result.set(iv, 0);
        result.set(new Uint8Array(ciphertextBuffer), CryptoManager.GCM_IV_LENGTH);

        return result.buffer;
    }

    /**
     * Decrypts data using AES-GCM
     *
     * @param key The CryptoKey to use for decryption
     * @param encryptedData The encrypted data with IV prepended (IV + ciphertext)
     * @returns Promise<ArrayBuffer> containing the decrypted plaintext
     * @throws Error if decryption fails
     */
    static async decrypt(key, encryptedData) {
        // Convert to Uint8Array if needed
        if (!(encryptedData instanceof Uint8Array)) {
            encryptedData = new Uint8Array(encryptedData);
        }

        // Extract IV and ciphertext
        if (encryptedData.length < CryptoManager.GCM_IV_LENGTH) {
            throw new Error("Encrypted data too short");
        }

        const iv = encryptedData.slice(0, CryptoManager.GCM_IV_LENGTH);
        const ciphertext = encryptedData.slice(CryptoManager.GCM_IV_LENGTH);

        // Decrypt
        try {
            const plaintextBuffer = await crypto.subtle.decrypt(
                {name: "AES-GCM", iv: iv},
                key,
                ciphertext
            );
            return plaintextBuffer;
        } catch (error) {
            throw new Error(`Decryption failed: ${error.message}`);
        }
    }

    /**
     * Encrypts a string using AES-GCM
     *
     * @param key The CryptoKey to use for encryption
     * @param plaintext The string to encrypt
     * @returns Promise<string> Base64-encoded encrypted data with IV prepended
     */
    static async encryptString(key, plaintext) {
        const encryptedBuffer = await CryptoManager.encrypt(key, plaintext);
        // Convert to base64 string
        const bytes = new Uint8Array(encryptedBuffer);
        let binary = '';
        for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary);
    }

    /**
     * Decrypts a string using AES-GCM
     *
     * @param key The CryptoKey to use for decryption
     * @param encryptedData Base64-encoded encrypted data with IV prepended
     * @returns Promise<string> Decrypted plaintext string
     * @throws Error if decryption fails
     */
    static async decryptString(key, encryptedData) {
        // Decode base64
        const binaryString = window.atob(encryptedData);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        const decryptedBuffer = await CryptoManager.decrypt(key, bytes.buffer);
        // Convert ArrayBuffer to string
        return new TextDecoder().decode(decryptedBuffer);
    }

    /**
     * Generates an HMAC-SHA256 signature
     *
     * @param key The CryptoKey to use for HMAC
     * @param data The data to sign (Uint8Array or ArrayBuffer)
     * @returns Promise<ArrayBuffer> HMAC signature
     */
    static async signHmac(key, data) {
        if (!(data instanceof Uint8Array)) {
            data = new Uint8Array(data);
        }
        return await crypto.subtle.sign(
            CryptoManager.HMAC_ALGORITHM,
            key,
            data
        );
    }

    /**
     * Verifies an HMAC-SHA256 signature
     *
     * @param key The CryptoKey to use for HMAC verification
     * @param signature The signature to verify (Uint8Array or ArrayBuffer)
     * @param data The original data (Uint8Array or ArrayBuffer)
     * @returns Promise<boolean> True if signature is valid
     */
    static async verifyHmac(key, signature, data) {
        if (!(signature instanceof Uint8Array)) {
            signature = new Uint8Array(signature);
        }
        if (!(data instanceof Uint8Array)) {
            data = new Uint8Array(data);
        }
        return await crypto.subtle.verify(
            CryptoManager.HMAC_ALGORITHM,
            key,
            signature,
            data
        );
    }

    /**
     * Computes HMAC-SHA256 and returns as hex string
     *
     * @param key The CryptoKey to use for HMAC
     * @param data The data to sign (Uint8Array or ArrayBuffer)
     * @returns Promise<string> Hex-encoded HMAC
     */
    static async computeHmacHex(key, data) {
        const signature = await CryptoManager.signHmac(key, data);
        return CryptoManager.bytesToHex(new Uint8Array(signature));
    }

    /**
     * Generates a random nonce as specified in PROTOCOL.md
     *
     * @param length Length of nonce in bytes (default 16 bytes for challenge)
     * @returns Uint8Array containing random nonce
     */
    static generateNonce(length = 16) {
        return crypto.getRandomValues(new Uint8Array(length));
    }

    /**
     * Generates a random shared secret for HMAC (32 bytes = 256 bits)
     *
     * @returns Uint8Array containing random secret
     */
    static generateSharedSecret(length = 32) {
        return crypto.getRandomValues(new Uint8Array(length));
    }

    /**
     * Converts bytes to hex string
     *
     * @param bytes The bytes (Uint8Array)
     * @returns string Hex string representation
     */
    static bytesToHex(bytes) {
        return Array.from(bytes)
            .map(b => b.toString(16).padStart(2, '0'))
            .join('')
            .toUpperCase();
    }

    /**
     * Converts hex string to bytes
     *
     * @param hex Hex string representation
     * @returns Uint8Array containing the bytes
     */
    static hexToBytes(hex) {
        if (hex.length % 2 !== 0) {
            throw new Error("Invalid hex string");
        }

        const bytes = new Uint8Array(hex.length / 2);
        for (let i = 0; i < hex.length; i += 2) {
            bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }

    /**
     * Converts nonce to hex string for display/storage
     *
     * @param nonce The nonce bytes (Uint8Array)
     * @returns string Hex string representation
     */
    static nonceToHex(nonce) {
        return CryptoManager.bytesToHex(nonce);
    }

    /**
     * Converts hex string to nonce bytes
     *
     * @param hex Hex string representation
     * @returns Uint8Array containing the nonce bytes
     */
    static hexToNonce(hex) {
        return CryptoManager.hexToBytes(hex);
    }

    /**
     * Converts bytes to base64url string (for QR code encoding)
     *
     * @param bytes The bytes (Uint8Array)
     * @returns string Base64url-encoded string
     */
    static bytesToBase64Url(bytes) {
        let binary = '';
        for (let i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary)
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=/g, '');
    }

    /**
     * Converts base64url string to bytes
     *
     * @param base64url Base64url-encoded string
     * @returns Uint8Array containing the bytes
     */
    static base64UrlToBytes(base64url) {
        let base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
        while (base64.length % 4) base64 += '=';

        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes;
    }

    /**
     * Derives a key from a password using PBKDF2
     * For deriving encryption keys from user passwords
     *
     * @param password The password string
     * @param salt The salt (Uint8Array, 16+ bytes recommended)
     * @param iterations PBKDF2 iterations (default 100000)
     * @returns Promise<CryptoKey> Derived key for AES-GCM
     */
    static async deriveKeyFromPassword(password, salt, iterations = 100000) {
        const encoder = new TextEncoder();
        const keyMaterial = await crypto.subtle.importKey(
            "raw",
            encoder.encode(password),
            "PBKDF2",
            false,
            ["deriveKey"]
        );

        return await crypto.subtle.deriveKey(
            {
                name: "PBKDF2",
                salt: salt,
                iterations: iterations,
                hash: "SHA-256"
            },
            keyMaterial,
            CryptoManager.ALGORITHM,
            true,
            ["encrypt", "decrypt"]
        );
    }
}

// Export for use in browser extension
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CryptoManager;
}