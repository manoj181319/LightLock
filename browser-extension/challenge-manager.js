/**
 * Challenge manager for LightLock Phase 2
 * Handles nonce generation, storage, and verification as per PROTOCOL.md
 * Implements the challenge-response mechanism with replay protection
 * Updated for QR code-based challenge delivery
 */

class ChallengeManager {
    // Constants from PROTOCOL.md
    static get NONCE_LENGTH() { return 16; } // 128-bit nonce
    static get REPLAY_WINDOW_MS() { return 5 * 60 * 1000; } // 5 minutes
    static get MAX_NONCES_TRACKED() { return 1000; } // Last 1000 nonces
    static get CHALLENGE_TTL_MS() { return 5 * 60 * 1000; } // 5 minutes challenge validity

    constructor() {
        // Initialize nonce storage (in practice, would use IndexedDB or similar)
        this.usedNonces = new Map(); // nonceHex -> timestamp
        this.storageKey = "lightlock-used-nonces";
        this.currentChallenge = null;
        this.challengeCreatedAt = null;
        this.loadUsedNonces();
    }

    /**
     * Generates a cryptographically random nonce using window.crypto.getRandomValues()
     * As per PROTOCOL.md: "Challenge Generation: The browser extension generates a
     * cryptographically random 16-byte nonce using window.crypto.getRandomValues()"
     *
     * @returns Uint8Array containing the nonce
     */
    static generateNonce() {
        return crypto.getRandomValues(new Uint8Array(this.NONCE_LENGTH));
    }

    /**
     * Converts nonce to hex string for storage/comparison
     *
     * @param nonce The nonce bytes (Uint8Array)
     * @returns string Hex string representation
     */
    static nonceToHex(nonce) {
        return Array.from(nonce)
            .map(b => b.toString(16).padStart(2, '0'))
            .join('')
            .toUpperCase();
    }

    /**
     * Converts hex string to nonce bytes
     *
     * @param hex Hex string representation
     * @returns Uint8Array containing the nonce bytes
     */
    static hexToNonce(hex) {
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
     * Converts nonce to base64url for QR code encoding
     *
     * @param nonce The nonce bytes (Uint8Array)
     * @returns string Base64url-encoded nonce
     */
    static nonceToBase64Url(nonce) {
        let binary = '';
        for (let i = 0; i < nonce.length; i++) {
            binary += String.fromCharCode(nonce[i]);
        }
        return btoa(binary)
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=/g, '');
    }

    /**
     * Converts base64url string to nonce bytes
     *
     * @param base64url Base64url-encoded string
     * @returns Uint8Array containing the nonce bytes
     */
    static base64UrlToNonce(base64url) {
        // Add padding back
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
     * Creates a QR code payload for the challenge
     * Format: { v: 1, n: <base64url_nonce>, k: <base64url_shared_key>, t: <timestamp>, e: <expiry_ms> }
     *
     * @param nonce The challenge nonce
     * @param sharedSecret Optional 32-byte shared key for optical session pairing
     * @returns Object QR code payload
     */
    static createQRPayload(nonce, sharedSecret = null) {
        const now = Date.now();
        const payload = {
            v: 1,                             // Version
            n: this.nonceToBase64Url(nonce),  // Nonce
            t: now,                           // Timestamp
            e: this.CHALLENGE_TTL_MS          // Expiry (ms)
        };
        if (sharedSecret) {
            payload.k = this.nonceToBase64Url(sharedSecret);
        }
        return payload;
    }

    /**
     * Parses a QR code payload and validates it
     *
     * @param payload Parsed JSON from QR code
     * @returns Object { valid: boolean, nonce: Uint8Array|null, error?: string }
     */
    static parseQRPayload(payload) {
        if (!payload || typeof payload !== 'object') {
            return { valid: false, nonce: null, error: 'Invalid payload format' };
        }

        if (payload.v !== 1) {
            return { valid: false, nonce: null, error: `Unsupported version: ${payload.v}` };
        }

        if (!payload.n || typeof payload.n !== 'string') {
            return { valid: false, nonce: null, error: 'Missing nonce in payload' };
        }

        let nonce;
        try {
            nonce = this.base64UrlToNonce(payload.n);
        } catch (e) {
            return { valid: false, nonce: null, error: 'Invalid nonce encoding' };
        }

        if (nonce.length !== this.NONCE_LENGTH) {
            return { valid: false, nonce: null, error: `Invalid nonce length: ${nonce.length}` };
        }

        const now = Date.now();
        const timestamp = payload.t || 0;
        const expiry = payload.e || this.CHALLENGE_TTL_MS;

        // Check if challenge is expired
        if (now - timestamp > expiry) {
            return { valid: false, nonce: null, error: 'Challenge expired' };
        }

        // Check if challenge is from future (clock skew protection)
        if (timestamp > now + 60000) { // Allow 1 minute future skew
            return { valid: false, nonce: null, error: 'Challenge timestamp invalid' };
        }

        return { valid: true, nonce, timestamp, expiry };
    }

    /**
     * Stores a used nonce for replay protection
     * As per PROTOCOL.md: "Nonce Tracking: The browser extension maintains a rolling window
     * of the last 1000 used nonces for a 5-minute window"
     *
     * @param nonceHex Hex string of the nonce to store
     */
    storeUsedNonce(nonceHex) {
        const now = Date.now();
        this.usedNonces.set(nonceHex, now);
        this.cleanupOldNonces();
        this.persistUsedNonces();
    }

    /**
     * Checks if a nonce has been used before (replay protection)
     * As per PROTOCOL.md: "Single-use Nonce: Each challenge nonce is accepted only once;
     * subsequent attempts with the same nonce are rejected."
     *
     * @param nonceHex Hex string of the nonce to check
     * @returns boolean True if nonce is fresh (not used), false if already used
     */
    isNonceFresh(nonceHex) {
        this.cleanupOldNonces(); // Clean up before checking
        return !this.usedNonces.has(nonceHex);
    }

    /**
     * Removes nonces older than the replay window
     * As per PROTOCOL.md: "Optional Timestamp: A timestamp may be included in the payload
     * to further limit the replay window."
     */
    cleanupOldNonces() {
        const now = Date.now();
        const expiryTime = now - this.REPLAY_WINDOW_MS;

        for (const [nonceHex, timestamp] of this.usedNonces.entries()) {
            if (timestamp < expiryTime) {
                this.usedNonces.delete(nonceHex);
            }
        }

        // Also limit to maximum number of tracked nonces
        if (this.usedNonces.size > this.MAX_NONCES_TRACKED) {
            // Sort by timestamp and remove oldest
            const sorted = Array.from(this.usedNonces.entries())
                .sort((a, b) => a[1] - b[1]); // Sort by timestamp ascending

            // Remove oldest entries
            const toRemove = this.usedNonces.size - this.MAX_NONCES_TRACKED;
            for (let i = 0; i < toRemove; i++) {
                this.usedNonces.delete(sorted[i][0]);
            }
        }
    }

    /**
     * Persists used nonces to storage (simplified - in practice would use IndexedDB)
     */
    persistUsedNonces() {
        try {
            const noncesArray = Array.from(this.usedNonces.entries());
            localStorage.setItem(this.storageKey, JSON.stringify(noncesArray));
        } catch (e) {
            console.warn("Failed to persist used nonces:", e);
        }
    }

    /**
     * Loads used nonces from storage
     */
    loadUsedNonces() {
        try {
            const stored = localStorage.getItem(this.storageKey);
            if (stored) {
                const noncesArray = JSON.parse(stored);
                this.usedNonces = new Map(noncesArray);
                this.cleanupOldNonces(); // Clean up any old entries on load
            }
        } catch (e) {
            console.warn("Failed to load used nonces:", e);
            this.usedNonces = new Map();
        }
    }

    /**
     * Generates a new challenge and sets it as current
     *
     * @param sharedSecret Optional 32-byte shared secret key
     * @returns Object { nonce: Uint8Array, hex: string, base64url: string, qrPayload: Object, expiresAt: number }
     */
    generateChallenge(sharedSecret = null) {
        const nonce = ChallengeManager.generateNonce();
        const hex = ChallengeManager.nonceToHex(nonce);
        const base64url = ChallengeManager.nonceToBase64Url(nonce);
        const qrPayload = ChallengeManager.createQRPayload(nonce, sharedSecret);
        const now = Date.now();
        const expiresAt = now + ChallengeManager.CHALLENGE_TTL_MS;

        this.currentChallenge = {
            nonce: nonce,
            hex: hex,
            base64url: base64url,
            sharedSecret: sharedSecret,
            qrPayload: qrPayload,
            createdAt: now,
            expiresAt: expiresAt
        };
        this.challengeCreatedAt = now;

        return this.currentChallenge;
    }

    /**
     * Gets the current challenge
     *
     * @returns Object|null Current challenge or null if none/expired
     */
    getCurrentChallenge() {
        if (!this.currentChallenge) return null;

        // Check if expired
        if (Date.now() > this.currentChallenge.expiresAt) {
            this.clearCurrentChallenge();
            return null;
        }

        return this.currentChallenge;
    }

    /**
     * Clears the current challenge
     */
    clearCurrentChallenge() {
        this.currentChallenge = null;
        this.challengeCreatedAt = null;
    }

    /**
     * Gets remaining time for current challenge in seconds
     *
     * @returns number Seconds remaining, or 0 if no challenge/expired
     */
    getChallengeRemainingSeconds() {
        const challenge = this.getCurrentChallenge();
        if (!challenge) return 0;

        const remaining = Math.max(0, Math.ceil((challenge.expiresAt - Date.now()) / 1000));
        return remaining;
    }

    /**
     * Validates a challenge nonce received from the phone
     * As per PROTOCOL.md: "Verification: The computer extracts the challenge from the
     * received payload and compares it with the originally generated nonce. A match,
     * combined with valid CRC-8, constitutes successful authentication."
     *
     * @param receivedNonceHex Hex string of the received challenge nonce
     * @returns Object {isValid: boolean, isFresh: boolean, error?: string}
     */
    validateChallengeNonce(receivedNonceHex) {
        const current = this.getCurrentChallenge();

        if (!current) {
            return { isValid: false, isFresh: false, error: 'No active challenge' };
        }

        const expectedHex = current.hex;
        const isValid = receivedNonceHex === expectedHex;
        const isFresh = isValid ? this.isNonceFresh(receivedNonceHex) : false;

        // If valid and fresh, store it to prevent replay
        if (isValid && isFresh) {
            this.storeUsedNonce(receivedNonceHex);
            this.clearCurrentChallenge(); // Challenge consumed
        }

        return { isValid, isFresh, error: isValid ? (isFresh ? null : 'Replay detected') : 'Nonce mismatch' };
    }

    /**
     * Verifies an HMAC response from the phone
     * The phone sends HMAC-SHA256(challenge, shared_secret) along with encrypted credential
     *
     * @param receivedHmacHex Hex string of the received HMAC
     * @param sharedSecret Uint8Array The shared secret key
     * @returns Promise<Object> { valid: boolean, error?: string }
     */
    async verifyHmacResponse(receivedHmacHex, sharedSecret) {
        const current = this.getCurrentChallenge();

        if (!current) {
            return { valid: false, error: 'No active challenge for HMAC verification' };
        }

        try {
            // Import the shared secret as a CryptoKey for HMAC
            const key = await crypto.subtle.importKey(
                'raw',
                sharedSecret,
                { name: 'HMAC', hash: 'SHA-256' },
                false,
                ['verify']
            );

            // Convert received HMAC hex to ArrayBuffer
            const receivedHmac = ChallengeManager.hexToNonce(receivedHmacHex); // Reuse hex parser

            // Compute expected HMAC
            const expectedHmacBuffer = await crypto.subtle.sign(
                'HMAC',
                key,
                current.nonce
            );
            const expectedHmac = new Uint8Array(expectedHmacBuffer);

            // Constant-time comparison
            const isValid = crypto.subtle.verify(
                'HMAC',
                key,
                expectedHmacBuffer,
                receivedHmac
            );

            return { valid: isValid, error: isValid ? null : 'HMAC verification failed' };
        } catch (e) {
            console.error('HMAC verification error:', e);
            return { valid: false, error: `Verification error: ${e.message}` };
        }
    }

    /**
     * Gets statistics about nonce tracking
     *
     * @returns Object { tracked: number, windowRemaining: number }
     */
    getStats() {
        this.cleanupOldNonces();
        const now = Date.now();
        const oldest = this.usedNonces.size > 0
            ? Math.min(...this.usedNonces.values())
            : now;
        const windowRemaining = Math.max(0, Math.ceil((oldest + this.REPLAY_WINDOW_MS - now) / 1000));

        return {
            tracked: this.usedNonces.size,
            windowRemaining: windowRemaining,
            maxTracked: this.MAX_NONCES_TRACKED
        };
    }
}

// Export for use in browser extension
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ChallengeManager;
}