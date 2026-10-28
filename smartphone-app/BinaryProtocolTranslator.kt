package com.lightlock.app

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

    companion object {
        // Constants from PROTOCOL.md
        private val PREAMBLE_BITS = 32
        private val LENGTH_BITS = 8
        private val END_BITS = 32
        private val CHECKSUM_BITS = 8
        
        private val PREAMBLE_PATTERN = 0xAAAAAAAA
        private val END_PATTERN = 0x55555555
        
        // CRC-8 parameters from PROTOCOL.md
        private val CRC8_POLYNOMIAL = 0x07
        private val CRC8_INITIAL_VALUE = 0x00
    }

    /**
     * Encodes binary data into a complete frame with preamble, length, payload, checksum, and end.
     * 
     * @param payload The payload data to encode
     * @return ByteArray containing the complete frame
     */
    fun encodeFrame(payload: ByteArray): ByteArray {
        // Calculate total frame size
        val preambleSize = PREAMBLE_BITS / 8 // 4 bytes
        val lengthSize = LENGTH_BITS / 8     // 1 byte
        val payloadSize = payload.size
        val checksumSize = CHECKSUM_BITS / 8 // 1 byte
        val endSize = END_BITS / 8           // 4 bytes
        
        val frameSize = preambleSize + lengthSize + payloadSize + checksumSize + endSize
        val frame = ByteArray(frameSize)
        
        var index = 0
        
        // Add preamble (4 bytes)
        frame[index++] = (PREAMBLE_PATTERN shr 24 and 0xFF).toByte()
        frame[index++] = (PREAMBLE_PATTERN shr 16 and 0xFF).toByte()
        frame[index++] = (PREAMBLE_PATTERN shr 8 and 0xFF).toByte()
        frame[index++] = (PREAMBLE_PATTERN and 0xFF).toByte()
        
        // Add length (1 byte)
        frame[index++] = payloadSize.toByte()
        
        // Add payload
        payload.copyTo(frame, index)
        index += payloadSize
        
        // Add checksum (CRC-8 of payload)
        val checksum = calculateCRC8(payload)
        frame[index++] = checksum
        
        // Add end pattern (4 bytes)
        frame[index++] = (END_PATTERN shr 24 and 0xFF).toByte()
        frame[index++] = (END_PATTERN shr 16 and 0xFF).toByte()
        frame[index++] = (END_PATTERN shr 8 and 0xFF).toByte()
        frame[index++] = (END_PATTERN and 0xFF).toByte()
        
        return frame
    }

    /**
     * Decodes a binary frame and extracts the payload if valid.
     * 
     * @param frame The complete frame to decode
     * @return Pair containing (isValid: Boolean, payload: ByteArray)
     *         If isValid is false, payload will be empty
     */
    fun decodeFrame(frame: ByteArray): Pair<Boolean, ByteArray> {
        // Minimum frame size: preamble(4) + length(1) + payload(0) + checksum(1) + end(4) = 10 bytes
        if (frame.size < 10) {
            return Pair(false, ByteArray(0))
        }
        
        var index = 0
        
        // Check preamble
        val preamble = (frame[index++] and 0xFF) shl 24 or
                       (frame[index++] and 0xFF) shl 16 or
                       (frame[index++] and 0xFF) shl 8 or
                       (frame[index++] and 0xFF)
        if (preamble != PREAMBLE_PATTERN) {
            return Pair(false, ByteArray(0))
        }
        
        // Get length
        val payloadSize = frame[index++] and 0xFF
        
        // Check if frame is large enough for declared payload
        val expectedSize = 4 + 1 + payloadSize + 1 + 4 // preamble + length + payload + checksum + end
        if (frame.size < expectedSize) {
            return Pair(false, ByteArray(0))
        }
        
        // Extract payload
        val payload = ByteArray(payloadSize)
        frame.copyTo(payload, 0, index, index + payloadSize)
        index += payloadSize
        
        // Verify checksum
        val receivedChecksum = frame[index++] and 0xFF
        val calculatedChecksum = calculateCRC8(payload)
        if (receivedChecksum != calculatedChecksum) {
            return Pair(false, ByteArray(0))
        }
        
        // Check end pattern
        val end = (frame[index++] and 0xFF) shl 24 or
                  (frame[index++] and 0xFF) shl 16 or
                  (frame[index++] and 0xFF) shl 8 or
                  (frame[index++] and 0xFF)
        if (end != END_PATTERN) {
            return Pair(false, ByteArray(0))
        }
        
        return Pair(true, payload)
    }

    /**
     * Calculates CRC-8 as specified in PROTOCOL.md
     * Polynomial: 0x07, Initial value: 0x00
     * 
     * @param data The data to calculate CRC-8 for
     * @return The CRC-8 checksum
     */
    private fun calculateCRC8(data: ByteArray): Int {
        var crc = CRC8_INITIAL_VALUE
        for (byte in data) {
            crc = crc xor (byte and 0xFF)
            for (bit in 0 until 8) {
                if ((crc and 0x01) != 0) {
                    crc = (crc shr 1) xor CRC8_POLYNOMIAL
                } else {
                    crc = crc shr 1
                }
            }
        }
        return crc and 0xFF
    }

    /**
     * Encodes a string to bytes using UTF-8 encoding
     * 
     * @param text The string to encode
     * @return ByteArray containing the UTF-8 encoded bytes
     */
    fun stringToBytes(text: String): ByteArray {
        return text.toByteArray(Charsets.UTF_8)
    }

    /**
     * Decodes bytes to a string using UTF-8 encoding
     * 
     * @param bytes The bytes to decode
     * @return The decoded string
     */
    fun bytesToString(bytes: ByteArray): String {
        return String(bytes, Charsets.UTF_8)
    }
}