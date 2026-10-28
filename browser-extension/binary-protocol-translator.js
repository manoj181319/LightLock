/**
 * Binary protocol translator for LightLock Phase 2
 * Handles encoding and decoding of binary frames as per PROTOCOL.md
 *
 * Frame structure:
 * [PREAMBLE (32 bits)] [LENGTH (8 bits)] [PAYLOAD (N×8 bits)] [CHECKSUM (8 bits)] [END (32 bits)]
 *
 * - PREAMBLE: Fixed pattern 0xAAAAAAAA (10101010 repeated)
 * - LENGTH: 8-bit unsigned integer (0-255 payload bytes)
 * - PAYLOAD: Application data
 * - CHECKSUM: CRC-8 over payload bytes only (polynomial 0x07)
 * - END: Fixed pattern 0x55555555 (01010101 repeated)
 */

class BinaryProtocolTranslator {
    // Constants from PROTOCOL.md
    static get PREAMBLE_BITS() { return 32; }
    static get LENGTH_BITS() { return 8; }
    static get END_BITS() { return 32; }
    static get CHECKSUM_BITS() { return 8; }

    static get PREAMBLE_PATTERN() { return 0xAAAAAAAA; }
    static get END_PATTERN() { return 0x55555555; }

    // CRC-8 parameters from PROTOCOL.md
    static get CRC8_POLYNOMIAL() { return 0x07; }
    static get CRC8_INITIAL_VALUE() { return 0x00; }

    /**
     * Encodes binary data into a complete frame with preamble, length, payload, checksum, and end.
     *
     * @param payload The payload data to encode (Uint8Array)
     * @return Uint8Array containing the complete frame
     */
    static encodeFrame(payload) {
        // Calculate total frame size
        const preambleSize = this.PREAMBLE_BITS / 8; // 4 bytes
        const lengthSize = this.LENGTH_BITS / 8;     // 1 byte
        const payloadSize = payload.length;
        const checksumSize = this.CHECKSUM_BITS / 8; // 1 byte
        const endSize = this.END_BITS / 8;           // 4 bytes

        const frameSize = preambleSize + lengthSize + payloadSize + checksumSize + endSize;
        const frame = new Uint8Array(frameSize);

        let index = 0;

        // Add preamble (4 bytes)
        frame[index++] = (this.PREAMBLE_PATTERN >>> 24) & 0xFF;
        frame[index++] = (this.PREAMBLE_PATTERN >>> 16) & 0xFF;
        frame[index++] = (this.PREAMBLE_PATTERN >>> 8) & 0xFF;
        frame[index++] = this.PREAMBLE_PATTERN & 0xFF;

        // Add length (1 byte)
        frame[index++] = payloadSize & 0xFF;

        // Add payload
        frame.set(payload, index);
        index += payloadSize;

        // Add checksum (CRC-8 of payload)
        const checksum = this.calculateCRC8(payload);
        frame[index++] = checksum;

        // Add end pattern (4 bytes)
        frame[index++] = (this.END_PATTERN >>> 24) & 0xFF;
        frame[index++] = (this.END_PATTERN >>> 16) & 0xFF;
        frame[index++] = (this.END_PATTERN >>> 8) & 0xFF;
        frame[index++] = this.END_PATTERN & 0xFF;

        return frame;
    }

    /**
     * Decodes a binary frame and extracts the payload if valid.
     *
     * @param frame The complete frame to decode (Uint8Array)
     * @return Object containing {isValid: boolean, payload: Uint8Array}
     *         If isValid is false, payload will be empty
     */
    static decodeFrame(frame) {
        // Minimum frame size: preamble(4) + length(1) + payload(0) + checksum(1) + end(4) = 10 bytes
        if (frame.length < 10) {
            return {isValid: false, payload: new Uint8Array(0)};
        }

        let index = 0;

        // Check preamble
        const preamble = (frame[index++] & 0xFF) << 24 |
                         (frame[index++] & 0xFF) << 16 |
                         (frame[index++] & 0xFF) << 8 |
                         (frame[index++] & 0xFF);
        if (preamble !== this.PREAMBLE_PATTERN) {
            return {isValid: false, payload: new Uint8Array(0)};
        }

        // Get length
        const payloadSize = frame[index++] & 0xFF;

        // Check if frame is large enough for declared payload
        const expectedSize = 4 + 1 + payloadSize + 1 + 4; // preamble + length + payload + checksum + end
        if (frame.length < expectedSize) {
            return {isValid: false, payload: new Uint8Array(0)};
        }

        // Extract payload
        const payload = frame.slice(index, index + payloadSize);
        index += payloadSize;

        // Verify checksum
        const receivedChecksum = frame[index++] & 0xFF;
        const calculatedChecksum = this.calculateCRC8(payload);
        if (receivedChecksum !== calculatedChecksum) {
            return {isValid: false, payload: new Uint8Array(0)};
        }

        // Check end pattern
        const end = (frame[index++] & 0xFF) << 24 |
                    (frame[index++] & 0xFF) << 16 |
                    (frame[index++] & 0xFF) << 8 |
                    (frame[index++] & 0xFF);
        if (end !== this.END_PATTERN) {
            return {isValid: false, payload: new Uint8Array(0)};
        }

        return {isValid: true, payload: payload};
    }

    /**
     * Calculates CRC-8 as specified in PROTOCOL.md
     * Polynomial: 0x07, Initial value: 0x00
     *
     * @param data The data to calculate CRC-8 for (Uint8Array)
     * @return The CRC-8 checksum
     */
    static calculateCRC8(data) {
        let crc = this.CRC8_INITIAL_VALUE;
        for (let i = 0; i < data.length; i++) {
            crc ^= data[i] & 0xFF;
            for (let bit = 0; bit < 8; bit++) {
                if ((crc & 0x01) !== 0) {
                    crc = (crc >>> 1) ^ this.CRC8_POLYNOMIAL;
                } else {
                    crc = crc >>> 1;
                }
            }
        }
        return crc & 0xFF;
    }

    /**
     * Encodes a string to bytes using UTF-8 encoding
     *
     * @param text The string to encode
     * @return Uint8Array containing the UTF-8 encoded bytes
     */
    static stringToBytes(text) {
        const encoder = new TextEncoder();
        return encoder.encode(text);
    }

    /**
     * Decodes bytes to a string using UTF-8 encoding
     *
     * @param bytes The bytes to decode (Uint8Array)
     * @return The decoded string
     */
    static bytesToString(bytes) {
        const decoder = new TextDecoder();
        return decoder.decode(bytes);
    }

    /**
     * Instance method to calculate CRC-8 (delegates to static method)
     * @param data The data to calculate CRC-8 for (Uint8Array)
     * @return The CRC-8 checksum
     */
    calculateCRC8(data) {
        return BinaryProtocolTranslator.calculateCRC8(data);
    }

    /**
     * Instance method to encode frame (delegates to static method)
     * @param payload The payload data to encode (Uint8Array)
     * @return Uint8Array containing the complete frame
     */
    encodeFrame(payload) {
        return BinaryProtocolTranslator.encodeFrame(payload);
    }

    /**
     * Instance method to decode frame (delegates to static method)
     * @param frame The complete frame to decode (Uint8Array)
     * @return Object containing {isValid: boolean, payload: Uint8Array}
     */
    decodeFrame(frame) {
        return BinaryProtocolTranslator.decodeFrame(frame);
    }

    /**
     * Instance method to convert string to bytes (delegates to static method)
     * @param text The string to encode
     * @return Uint8Array containing the UTF-8 encoded bytes
     */
    stringToBytes(text) {
        return BinaryProtocolTranslator.stringToBytes(text);
    }

    /**
     * Instance method to convert bytes to string (delegates to static method)
     * @param bytes The bytes to decode (Uint8Array)
     * @return The decoded string
     */
    bytesToString(bytes) {
        return BinaryProtocolTranslator.bytesToString(bytes);
    }
}

// Export for use in browser extension
if (typeof module !== 'undefined' && module.exports) {
    module.exports = BinaryProtocolTranslator;
}