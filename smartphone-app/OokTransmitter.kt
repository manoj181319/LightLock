package com.lightlock.app

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * OOK Transmitter for LightLock Phase 2
 * Transmits binary data via screen flashing (OOK - On-Off Keying) at 20 bps
 * Uses full-screen white/black frames to encode bits
 */
class OokTransmitter private constructor(
    private val context: Context,
    private val rootView: View,
    private val onTransmissionComplete: (Boolean) -> Unit,
    private val onProgress: (Int, Int) -> Unit // currentBit, totalBits
) {

    companion object {
        // 20 bps timing: 50ms per bit
        const val BIT_DURATION_MS = 50L
        const val PRE_TRANSMIT_DELAY_MS = 1000L // Delay before starting transmission
        const val POST_TRANSMIT_DELAY_MS = 500L // Delay after transmission ends

        // Frame structure constants (must match receiver)
        const val PREAMBLE_BITS = 32
        const val LENGTH_BITS = 8
        const val END_BITS = 32
        const val PREAMBLE_PATTERN = 0xAAAAAAAA.toInt()
        const val END_PATTERN = 0x55555555

        /**
         * Factory method to create an OokTransmitter
         */
        fun create(
            context: Context,
            rootView: View,
            onTransmissionComplete: (Boolean) -> Unit,
            onProgress: (Int, Int) -> Unit
        ): OokTransmitter {
            return OokTransmitter(context, rootView, onTransmissionComplete, onProgress)
        }
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTransmitting = false
    private var transmissionView: View? = null
    private var originalFlags = 0

    /**
     * Transmit a payload using OOK optical signaling
     *
     * @param payload The payload bytes to transmit
     */
    fun transmit(payload: ByteArray) {
        if (isTransmitting) {
            onTransmissionComplete(false)
            return
        }

        isTransmitting = true

        // Encode payload into full frame
        val frame = BinaryProtocolTranslator.encodeFrame(payload)
        val bits = frameToBits(frame)

        // Run transmission on background thread
        executor.execute {
            try {
                // Prepare transmission view on main thread
                mainHandler.post {
                    setupTransmissionView()
                }

                // Pre-transmit delay
                Thread.sleep(PRE_TRANSMIT_DELAY_MS)

                // Transmit bits
                for (i in bits.indices) {
                    if (!isTransmitting) break

                    val bit = bits[i]
                    transmitBit(bit)

                    // Update progress on main thread
                    mainHandler.post {
                        onProgress(i + 1, bits.size)
                    }

                    // Bit timing
                    Thread.sleep(BIT_DURATION_MS)
                }

                // Post-transmit delay
                Thread.sleep(POST_TRANSMIT_DELAY_MS)

                // Cleanup on main thread
                mainHandler.post {
                    cleanupTransmissionView()
                    onTransmissionComplete(true)
                }
            } catch (e: InterruptedException) {
                mainHandler.post {
                    cleanupTransmissionView()
                    onTransmissionComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    cleanupTransmissionView()
                    onTransmissionComplete(false)
                }
            }
        }
    }

    /**
     * Cancel ongoing transmission
     */
    fun cancel() {
        isTransmitting = false
        mainHandler.post {
            cleanupTransmissionView()
        }
    }

    /**
     * Convert frame bytes to bit array (MSB first)
     */
    private fun frameToBits(frame: ByteArray): BooleanArray {
        val bits = BooleanArray(frame.size * 8)
        var bitIndex = 0
        for (b in frame) {
            for (shift in 7 downTo 0) {
                bits[bitIndex] = ((b shr shift) and 1) == 1
                bitIndex++
            }
        }
        return bits
    }

    /**
     * Transmit a single bit (true = white/on, false = black/off)
     */
    private fun transmitBit(bit: Boolean) {
        mainHandler.post {
            transmissionView?.setBackgroundColor(if (bit) Color.WHITE else Color.BLACK)
        }
    }

    /**
     * Setup full-screen transmission view
     */
    private fun setupTransmissionView() {
        // Create a full-screen view for transmission
        transmissionView = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // Add to root view
        (rootView as? FrameLayout)?.addView(transmissionView!!)

        // Keep screen on and fullscreen
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        originalFlags = activity?.window?.attributes?.flags ?: 0
    }

    /**
     * Cleanup transmission view
     */
    private fun cleanupTransmissionView() {
        transmissionView?.let { view ->
            (rootView as? FrameLayout)?.removeView(view)
            transmissionView = null
        }

        val activity = context as? android.app.Activity
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (originalFlags != 0) {
            activity?.window?.addFlags(originalFlags)
        }

        isTransmitting = false
    }

    /**
     * Shutdown executor
     */
    fun shutdown() {
        executor.shutdown()
    }
}