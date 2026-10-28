package com.lightlock.app

/**
 * Simple test for BinaryProtocolTranslator
 * This is a basic functionality test to verify encoding/decoding works
 */
class BinaryProtocolTranslatorTest {

    fun testEncodeDecode() {
        val translator = BinaryProtocolTranslator()
        
        // Test data
        val testString = "Hello, World! Test credential: user@example.com"
        val payload = translator.stringToBytes(testString)
        
        // Encode frame
        val frame = translator.encodeFrame(payload)
        
        // Decode frame
        val (isValid, decodedPayload) = translator.decodeFrame(frame)
        
        // Verify
        if (!isValid) {
            throw AssertionError("Frame decoding failed: Invalid frame")
        }
        
        val decodedString = translator.bytesToString(decodedPayload)
        if (decodedString != testString) {
            throw AssertionError("Data mismatch. Expected: '$testString', Got: '$decodedString'")
        }
        
        println("Test passed: BinaryProtocolTranslator encoding/decoding works correctly")
    }

    fun testCRCErrorDetection() {
        val translator = BinaryProtocolTranslator()
        
        // Test data
        val testString = "Test data for CRC"
        val payload = translator.stringToBytes(testString)
        
        // Encode frame
        val frame = translator.encodeFrame(payload)
        
        // Corrupt the frame by flipping a bit in the payload
        if (frame.size > 10) { // Make sure we have enough bytes
            val corruptedFrame = frame.clone()
            // Flip a bit in the payload area (after preamble + length)
            corruptedFrame[9] = (corruptedFrame[9] xor 0x01).toByte()
            
            // Decode corrupted frame
            val (isValid, _) = translator.decodeFrame(corruptedFrame)
            
            if (isValid) {
                throw AssertionError("CRC failed to detect corruption")
            }
            
            println("Test passed: CRC-8 correctly detects frame corruption")
        }
    }

    fun testFrameStructure() {
        val translator = BinaryProtocolTranslator()
        
        // Test with empty payload
        val emptyPayload = ByteArray(0)
        val emptyFrame = translator.encodeFrame(emptyPayload)
        
        // Expected size: preamble(4) + length(1) + payload(0) + checksum(1) + end(4) = 10 bytes
        if (emptyFrame.size != 10) {
            throw AssertionError("Empty payload frame size incorrect. Expected 10, got ${emptyFrame.size}")
        }
        
        // Test with small payload
        val smallPayload = translator.stringToBytes("A")
        val smallFrame = translator.encodeFrame(smallPayload)
        
        // Expected size: 4 + 1 + 1 + 1 + 4 = 11 bytes
        if (smallFrame.size != 11) {
            throw AssertionError("Small payload frame size incorrect. Expected 11, got ${smallFrame.size}")
        }
        
        println("Test passed: Frame structure is correct")
    }
}

// Run tests
fun main() {
    val test = BinaryProtocolTranslatorTest()
    try {
        test.testFrameStructure()
        test.testEncodeDecode()
        test.testCRCErrorDetection()
        println("All tests passed!")
    } catch (e: Exception) {
        println("Test failed: ${e.message}")
        e.printStackTrace()
    }
}