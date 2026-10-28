// Popup logic for LightLock Receiver extension
// Phase 2: Binary optical communication at 8 bps with QR code challenge flow

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const cameraPreview    = document.getElementById('cameraPreview');
    const cameraPlaceholder= document.getElementById('cameraPlaceholder');
    const waveformCanvas   = document.getElementById('waveformCanvas');
    const qrSection        = document.getElementById('qrSection');
    const qrCode           = document.getElementById('qrCode');
    const qrStatus         = document.getElementById('qrStatus');
    // Credential result panel (replaces old passwordSection / passwordContainer)
    const credentialSection= document.getElementById('credentialSection');
    const credentialValue  = document.getElementById('credentialValue');
    const credentialVerified=document.getElementById('credentialVerified');
    const credentialDecrypted=document.getElementById('credentialDecrypted');
    const credentialTime   = document.getElementById('credentialTime');
    const copyBtn          = document.getElementById('copyBtn');
    const copyIcon         = document.getElementById('copyIcon');
    const copyHint         = document.getElementById('copyHint');
    const dismissBtn       = document.getElementById('dismissBtn');
    const logContent       = document.getElementById('logContent');
    const logToggle        = document.getElementById('logToggle');
    const statusDot        = document.getElementById('statusDot');
    const statusText       = document.getElementById('statusText');
    const startBtn         = document.getElementById('startBtn');
    const stopBtn          = document.getElementById('stopBtn');

    // Binary protocol components
    const binaryTranslator = new BinaryProtocolTranslator();
    const challengeManager = new ChallengeManager();

    // ─── OOK demodulation: 8 bps = 125 ms/bit ────────────────────────────────
    const BIT_DURATION_MS = 125;
    let lastBitSampleTime = 0;
    let windowBrightnessSamples = [];
    let minBrightness = 255;
    let maxBrightness = 0;

    // Shift register & frame state
    let shiftRegister = 0;
    let receivingFrame = false;
    let framePhase = 'IDLE';
    let bitAccumulator = 0;
    let bitsInAccumulator = 0;
    let expectedPayloadLength = 0;
    let payloadBytes = [];
    let checksumByte = 0;

    // Challenge-response state
    // We keep a snapshot of the challenge that was used to generate the current QR,
    // so it remains available even after challengeManager clears it during validation.
    let activeChallenge = null;   // { hex, sharedSecret(Uint8Array) }
    let awaitingResponse = false;

    // Waveform visualization state
    let waveformCtx = null;
    const WAVEFORM_HEIGHT = 60;
    const WAVEFORM_HISTORY_SECONDS = 10;
    const WAVEFORM_SAMPLE_RATE = 30;
    const WAVEFORM_MAX_SAMPLES = WAVEFORM_HISTORY_SECONDS * WAVEFORM_SAMPLE_RATE;
    let waveformSamples = new Array(WAVEFORM_MAX_SAMPLES).fill(0);
    let waveformAnimationId = null;

    // Camera/streaming state
    let isStreaming = false;
    let mediaStream = null;
    let animationFrameId = null;
    let expiryUpdateIntervalId = null;

    // Frame log
    let logExpanded = true;
    const MAX_LOG_ENTRIES = 50;

    // ─── Waveform ─────────────────────────────────────────────────────────────
    function initWaveform() {
        const container = waveformCanvas.parentElement;
        const width = container.clientWidth;
        waveformCanvas.width = width * window.devicePixelRatio;
        waveformCanvas.height = WAVEFORM_HEIGHT * window.devicePixelRatio;
        waveformCanvas.style.width = width + 'px';
        waveformCanvas.style.height = WAVEFORM_HEIGHT + 'px';
        waveformCtx = waveformCanvas.getContext('2d');
        waveformCtx.scale(window.devicePixelRatio, window.devicePixelRatio);
        drawWaveform();
    }

    function drawWaveform() {
        if (!waveformCtx) return;
        const ctx = waveformCtx;
        const width = waveformCanvas.width / window.devicePixelRatio;
        const height = WAVEFORM_HEIGHT;

        ctx.clearRect(0, 0, width, height);
        ctx.fillStyle = 'rgba(10,15,20,0.05)';
        ctx.fillRect(0, 0, width, height);

        ctx.strokeStyle = 'rgba(206,212,218,0.2)';
        ctx.lineWidth = 0.5;
        for (let x = 0; x <= width; x += 50) {
            ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, height); ctx.stroke();
        }
        for (let y = height * 0.25; y < height; y += height * 0.25) {
            ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(width, y); ctx.stroke();
        }

        const thresholdY = height * 0.5;
        ctx.strokeStyle = 'rgba(245,166,35,0.4)';
        ctx.lineWidth = 1;
        ctx.setLineDash([4, 4]);
        ctx.beginPath(); ctx.moveTo(0, thresholdY); ctx.lineTo(width, thresholdY); ctx.stroke();
        ctx.setLineDash([]);

        if (waveformSamples.some(s => s > 0)) {
            ctx.strokeStyle = '#00D4AA';
            ctx.lineWidth = 2;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';
            ctx.beginPath();
            for (let i = 0; i < waveformSamples.length; i++) {
                const x = (i / (waveformSamples.length - 1)) * width;
                const y = height - (waveformSamples[i] * height * 0.9);
                i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
            }
            ctx.stroke();
        }

        ctx.strokeStyle = '#00D4AA';
        ctx.lineWidth = 1;
        ctx.setLineDash([4, 4]);
        ctx.beginPath(); ctx.moveTo(width, 0); ctx.lineTo(width, height); ctx.stroke();
        ctx.setLineDash([]);
    }

    function addWaveformSample(brightness) {
        waveformSamples.shift();
        waveformSamples.push(Math.max(0, Math.min(1, brightness)));
    }

    function animateWaveform() {
        drawWaveform();
        waveformAnimationId = requestAnimationFrame(animateWaveform);
    }

    // ─── Status / Log ─────────────────────────────────────────────────────────
    function updateStatus(status, type = 'info') {
        statusText.textContent = status;
        statusDot.className = 'status-dot';
        if (type === 'active') statusDot.classList.add('active');
        else if (type === 'warning') statusDot.classList.add('warning');
        else if (type === 'error') statusDot.classList.add('error');
    }

    function addLogEntry(message, type = 'info') {
        const emptyMsg = logContent.querySelector('.log-empty');
        if (emptyMsg) emptyMsg.remove();

        const entry = document.createElement('div');
        entry.className = 'log-entry';

        const timeDiv = document.createElement('div');
        timeDiv.className = 'log-time';
        timeDiv.textContent = new Date().toLocaleTimeString();

        const msgDiv = document.createElement('div');
        msgDiv.className = `log-message ${type}`;
        msgDiv.textContent = message;

        entry.appendChild(timeDiv);
        entry.appendChild(msgDiv);
        logContent.insertBefore(entry, logContent.firstChild);

        while (logContent.children.length > MAX_LOG_ENTRIES) {
            logContent.removeChild(logContent.lastChild);
        }
    }

    // ─── QR Code ──────────────────────────────────────────────────────────────
    function updateQrDisplay(challenge) {
        if (!challenge) {
            qrSection.classList.add('hidden');
            clearCanvasEl(qrCode);
            return;
        }
        qrSection.classList.remove('hidden');
        generateQRCode(challenge.qrPayload);
        qrStatus.textContent = 'Scan with LightLock app';
        qrStatus.classList.add('active');
    }

    function clearCanvasEl(canvas) {
        if (canvas instanceof HTMLCanvasElement) {
            const ctx = canvas.getContext('2d');
            if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
    }

    // FIX 2 – On expiry: auto-regenerate instead of just clearing
    function updateExpiryDisplay() {
        const remaining = challengeManager.getChallengeRemainingSeconds();
        const challenge = challengeManager.getCurrentChallenge();

        if (!challenge || remaining <= 0) {
            // Auto-regenerate a fresh challenge
            if (isStreaming && awaitingResponse) {
                addLogEntry('Challenge expired — generating new QR', 'warning');
                startChallengeCycle();
            } else {
                challengeManager.clearCurrentChallenge();
                qrSection.classList.add('hidden');
                clearCanvasEl(qrCode);
                if (expiryUpdateIntervalId) {
                    clearInterval(expiryUpdateIntervalId);
                    expiryUpdateIntervalId = null;
                }
            }
            return;
        }

        const mins = Math.floor(remaining / 60);
        const secs = remaining % 60;
        qrStatus.textContent = `Scan with LightLock app (${mins}:${secs.toString().padStart(2, '0')})`;
    }

    // FIX 1 – size matches the 264×264 canvas we set in HTML
    function generateQRCode(payload) {
        try {
            if (typeof QRious === 'undefined') {
                qrStatus.textContent = 'QR library not loaded';
                return;
            }
            const jsonString = JSON.stringify(payload);
            new QRious({
                element: qrCode,
                value: jsonString,
                size: 264,          // matches .qr-code CSS width/height
                level: 'M',
                background: 'white',
                foreground: '#0A0F14',
                padding: 4          // minimal padding so modules are as large as possible
            });
        } catch (e) {
            console.error('QR generation error:', e);
            qrStatus.textContent = 'QR generation failed: ' + e.message;
        }
    }

    // ─── Credential result panel ──────────────────────────────────────────────
    function showCredential(password, hmacOk, decryptOk) {
        credentialValue.textContent = password;
        credentialTime.textContent = new Date().toLocaleTimeString();

        credentialVerified.textContent = hmacOk  ? 'verified ✓' : 'FAILED ✗';
        credentialVerified.className   = hmacOk  ? 'meta-verified' : 'meta-unverified';
        credentialDecrypted.textContent= decryptOk ? 'yes ✓' : 'no (raw hex)';
        credentialDecrypted.className  = decryptOk ? 'meta-verified' : 'meta-unverified';

        copyHint.textContent = 'Click ◈ to copy, then paste into the password field';
        copyHint.className = 'copy-hint';
        credentialSection.classList.remove('hidden');

        // Auto-copy to clipboard
        copyToClipboard(password);

        addLogEntry('Credential shown — copied to clipboard', 'success');
    }

    function copyToClipboard(text) {
        navigator.clipboard.writeText(text).then(() => {
            copyHint.textContent = '✓ Copied to clipboard!';
            copyHint.className = 'copy-hint copied';
            // Swap icon to a checkmark briefly
            copyIcon.innerHTML = '<polyline points="20 6 9 17 4 12" stroke-width="2.5"/>';
            setTimeout(() => {
                copyIcon.innerHTML = '<rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>';
                copyHint.textContent = 'Click ◈ to copy again, then paste into the password field';
                copyHint.className = 'copy-hint';
            }, 2000);
        }).catch(() => {
            // Fallback: select the text so user can Ctrl+C
            const range = document.createRange();
            range.selectNodeContents(credentialValue);
            window.getSelection().removeAllRanges();
            window.getSelection().addRange(range);
            copyHint.textContent = 'Auto-copy failed — text selected, press Ctrl+C';
        });
    }

    function clearCredentialDisplay() {
        credentialSection.classList.add('hidden');
        credentialValue.textContent = '';
    }

    // ─── Video frame processing ───────────────────────────────────────────────
    // Persistent off-screen canvas reused every frame (avoids GC pressure)
    let offscreenCanvas = null;
    let offscreenCtx = null;

    function processVideoFrame() {
        if (!isStreaming || !cameraPreview || cameraPreview.readyState < 2) {
            animationFrameId = requestAnimationFrame(processVideoFrame);
            return;
        }

        try {
            const vw = cameraPreview.videoWidth;
            const vh = cameraPreview.videoHeight;

            // Reuse offscreen canvas
            if (!offscreenCanvas || offscreenCanvas.width !== vw || offscreenCanvas.height !== vh) {
                offscreenCanvas = document.createElement('canvas');
                offscreenCanvas.width = vw;
                offscreenCanvas.height = vh;
                offscreenCtx = offscreenCanvas.getContext('2d', { willReadFrequently: true });
            }

            offscreenCtx.drawImage(cameraPreview, 0, 0, vw, vh);

            // Sample a ~100 representative pixels for brightness
            const sampleStep = Math.max(1, Math.floor((vw * vh) / 100));
            let totalBrightness = 0;
            let samples = 0;

            // Flat pixel walk (avoid repeated getImageData calls - use full image once)
            const imageData = offscreenCtx.getImageData(0, 0, vw, vh);
            const d = imageData.data;
            for (let i = 0; i < d.length; i += sampleStep * 4) {
                totalBrightness += 0.299 * d[i] + 0.587 * d[i + 1] + 0.114 * d[i + 2];
                samples++;
            }

            const avgBrightness = samples > 0 ? totalBrightness / samples : 0;
            const normalizedBrightness = avgBrightness / 255.0;
            const currentTime = Date.now();

            // Adaptive threshold (running min/max with slow decay)
            if (avgBrightness < minBrightness) minBrightness = avgBrightness;
            else minBrightness = minBrightness * 0.995 + avgBrightness * 0.005;
            if (avgBrightness > maxBrightness) maxBrightness = avgBrightness;
            else maxBrightness = maxBrightness * 0.995 + avgBrightness * 0.005;

            const dynamicRange = maxBrightness - minBrightness;
            const threshold = dynamicRange > 25 ? (minBrightness + maxBrightness) / 2 : 128;

            addWaveformSample(normalizedBrightness);
            windowBrightnessSamples.push(avgBrightness);

            // Decide a bit once per 50 ms window
            if (currentTime - lastBitSampleTime >= BIT_DURATION_MS) {
                const bitAvg = windowBrightnessSamples.reduce((a, b) => a + b, 0) / windowBrightnessSamples.length;
                windowBrightnessSamples = [];
                lastBitSampleTime = currentTime;

                const bit = bitAvg >= threshold ? 1 : 0;
                processOokBit(bit);
            }

            animationFrameId = requestAnimationFrame(processVideoFrame);
        } catch (error) {
            console.error('Frame processing error:', error);
            updateStatus(`Error: ${error.message}`, 'error');
            animationFrameId = requestAnimationFrame(processVideoFrame);
        }
    }

    // ─── OOK demodulator ─────────────────────────────────────────────────────
    function processOokBit(bit) {
        if (!receivingFrame) {
            shiftRegister = ((shiftRegister << 1) | bit) >>> 0;
            if (shiftRegister === 0xAAAAAAAA) {
                receivingFrame = true;
                framePhase = 'LENGTH';
                bitAccumulator = 0;
                bitsInAccumulator = 0;
                expectedPayloadLength = 0;
                payloadBytes = [];
                checksumByte = 0;
                addLogEntry('Preamble detected (0xAAAAAAAA)', 'info');
                updateStatus('Receiving frame…', 'active');
            }
        } else {
            bitAccumulator = ((bitAccumulator << 1) | bit) >>> 0;
            bitsInAccumulator++;

            if (framePhase === 'LENGTH' && bitsInAccumulator === 8) {
                expectedPayloadLength = bitAccumulator & 0xFF;
                bitAccumulator = 0;
                bitsInAccumulator = 0;
                addLogEntry(`Frame length: ${expectedPayloadLength} bytes`, 'info');

                if (expectedPayloadLength === 0 || expectedPayloadLength > 255) {
                    addLogEntry('Invalid frame length — resetting', 'error');
                    resetReceptionState();
                } else {
                    framePhase = 'PAYLOAD';
                }

            } else if (framePhase === 'PAYLOAD' && bitsInAccumulator === 8) {
                payloadBytes.push(bitAccumulator & 0xFF);
                bitAccumulator = 0;
                bitsInAccumulator = 0;
                if (payloadBytes.length === expectedPayloadLength) framePhase = 'CHECKSUM';

            } else if (framePhase === 'CHECKSUM' && bitsInAccumulator === 8) {
                checksumByte = bitAccumulator & 0xFF;
                bitAccumulator = 0;
                bitsInAccumulator = 0;
                framePhase = 'END';

            } else if (framePhase === 'END' && bitsInAccumulator === 32) {
                const endPattern = bitAccumulator >>> 0;
                if (endPattern !== 0x55555555) {
                    addLogEntry(`End pattern mismatch (0x${endPattern.toString(16).toUpperCase()})`, 'error');
                    resetReceptionState();
                    return;
                }

                const payloadArray = new Uint8Array(payloadBytes);
                const calculatedCrc = binaryTranslator.calculateCRC8(payloadArray);
                if (calculatedCrc !== checksumByte) {
                    addLogEntry(`CRC-8 mismatch (recv 0x${checksumByte.toString(16)} calc 0x${calculatedCrc.toString(16)})`, 'error');
                    resetReceptionState();
                    return;
                }

                addLogEntry('Frame verified ✓ (Preamble + CRC-8 + End)', 'success');
                handleValidFrame(payloadArray);
                resetReceptionState();
            }
        }
    }

    function resetReceptionState() {
        receivingFrame = false;
        framePhase = 'IDLE';
        shiftRegister = 0;
        bitAccumulator = 0;
        bitsInAccumulator = 0;
        expectedPayloadLength = 0;
        payloadBytes = [];
        checksumByte = 0;
        if (isStreaming && awaitingResponse) {
            updateStatus('Scanning for optical signal…', 'active');
        }
    }

    // ─── Frame handler ────────────────────────────────────────────────────────
    // FIX 3 (compact payload) – the phone now sends:
    //   [HMAC-SHA256 truncated to 8 bytes (8B)] [AES-GCM IV (12B)] [ciphertext+tag]
    // We derive the AES key from the ephemeral shared secret embedded in the QR code.
    // The nonce is NOT echoed in the frame (browser already has it from the QR).
    // This saves 16 (nonce echo) + 24 (HMAC tail) = 40 bytes versus the old format,
    // reducing a typical transmission from ~34 s to ~18 s at 20 bps.
    //
    // FIX 2 – we use activeChallenge (snapshot taken at startChallengeCycle) instead
    // of challengeManager.currentChallenge so the nonce is still available after
    // challengeManager.validateChallengeNonce() clears it during validation.
    async function handleValidFrame(payload) {
        addLogEntry(`Frame received — ${payload.length} bytes`, 'success');

        if (!activeChallenge) {
            addLogEntry('No active challenge — ignoring frame', 'warning');
            return;
        }

        // ── Compact format: [hmac8(8B)][iv(12B)][ciphertext+tag] ──────────────
        const MIN_COMPACT = 8 + 12 + 16 + 1; // hmac8 + iv + gcm-tag + ≥1 byte pw
        // ── Legacy format: [nonce(16B)][hmac32(32B)][iv(12B)][ciphertext+tag] ─
        const MIN_LEGACY  = 16 + 32 + 12 + 16 + 1;

        const isCompact = payload.length >= MIN_COMPACT && payload.length < MIN_LEGACY;
        const isLegacy  = payload.length >= MIN_LEGACY;

        if (!isCompact && !isLegacy) {
            addLogEntry(`Payload too short (${payload.length} bytes)`, 'warning');
            return;
        }

        const sharedSecretBytes = activeChallenge.sharedSecret; // Uint8Array, 32 bytes
        const expectedNonceHex = activeChallenge.hex;

        let hmacInput, hmacReceived, iv, ciphertext;

        if (isLegacy) {
            // Old format (phone sends nonce echo + full 32-byte HMAC)
            const receivedNonce = payload.slice(0, 16);
            const receivedNonceHex = bytesToHex(receivedNonce).toUpperCase();

            if (receivedNonceHex !== expectedNonceHex.toUpperCase()) {
                addLogEntry('Nonce mismatch — ignoring frame', 'error');
                return;
            }
            hmacReceived = payload.slice(16, 48);  // 32 bytes
            iv           = payload.slice(48, 60);  // 12 bytes
            ciphertext   = payload.slice(60);
            hmacInput    = receivedNonce;           // HMAC was computed over the nonce
        } else {
            // Compact format
            hmacReceived = payload.slice(0, 8);    // 8-byte truncated HMAC
            iv           = payload.slice(8, 20);   // 12-byte IV
            ciphertext   = payload.slice(20);
            // HMAC was computed over the raw nonce bytes (decoded from QR base64url)
            hmacInput    = base64UrlToBytes(activeChallenge.base64url);
        }

        // ── Verify HMAC ──────────────────────────────────────────────────────
        let hmacValid = false;
        try {
            const hmacKey = await crypto.subtle.importKey(
                'raw', sharedSecretBytes,
                { name: 'HMAC', hash: 'SHA-256' },
                false, ['sign']
            );
            const fullHmac = new Uint8Array(await crypto.subtle.sign('HMAC', hmacKey, hmacInput));
            const truncLen = isLegacy ? 32 : 8;
            hmacValid = fullHmac.slice(0, truncLen).every((b, i) => b === hmacReceived[i]);
            addLogEntry(hmacValid ? 'HMAC verified ✓' : 'HMAC mismatch!', hmacValid ? 'success' : 'error');
            if (!hmacValid) { awaitingResponse = false; startChallengeCycle(); return; }
        } catch (e) {
            addLogEntry(`HMAC error: ${e.message}`, 'error');
            return;
        }

        // ── Decrypt credential ────────────────────────────────────────────────
        let decryptedPassword = null;
        try {
            const aesKey = await crypto.subtle.importKey(
                'raw', sharedSecretBytes,
                { name: 'AES-GCM' },
                false, ['decrypt']
            );
            const plaintextBuffer = await crypto.subtle.decrypt(
                { name: 'AES-GCM', iv: iv },
                aesKey,
                ciphertext
            );
            decryptedPassword = new TextDecoder().decode(plaintextBuffer);
            addLogEntry('Credential decrypted ✓', 'success');
        } catch (e) {
            addLogEntry(`Decryption failed: ${e.message}`, 'error');
        }

        if (decryptedPassword) {
            showCredential(decryptedPassword, hmacValid, true);
        } else {
            showCredential(bytesToHex(ciphertext), hmacValid, false);
        }

        // Consume this challenge; start a fresh one for next use
        challengeManager.clearCurrentChallenge();
        activeChallenge = null;
        awaitingResponse = false;
        clearQrDisplay();
        updateStatus('Response received ✓', 'active');
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    function bytesToHex(arr) {
        return Array.from(arr).map(b => b.toString(16).padStart(2, '0')).join('').toUpperCase();
    }

    function base64UrlToBytes(b64url) {
        let b64 = b64url.replace(/-/g, '+').replace(/_/g, '/');
        while (b64.length % 4) b64 += '=';
        const bin = atob(b64);
        return Uint8Array.from(bin, c => c.charCodeAt(0));
    }

    // ─── Camera ───────────────────────────────────────────────────────────────
    async function startCamera() {
        try {
            updateStatus('Requesting camera access…', 'warning');
            startBtn.disabled = true;

            mediaStream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: 'environment' }
            });

            cameraPreview.srcObject = mediaStream;
            await new Promise(resolve => {
                if (cameraPreview.readyState >= 2) resolve();
                else cameraPreview.onloadedmetadata = resolve;
            });

            cameraPreview.classList.add('active');
            cameraPlaceholder.classList.add('hidden');
            waveformCanvas.classList.remove('hidden');
            isStreaming = true;
            updateStatus('Scanning for optical signal…', 'active');

            initWaveform();
            animateWaveform();

            cancelAnimationFrame(animationFrameId);
            animationFrameId = requestAnimationFrame(processVideoFrame);

            startChallengeCycle();
            stopBtn.disabled = false;
        } catch (error) {
            console.error('Camera error:', error);
            const msgs = {
                NotAllowedError: 'Camera access denied. Check browser permissions.',
                NotFoundError: 'No camera found on this device.',
                NotReadableError: 'Camera is in use or not accessible.',
                OverconstrainedError: 'Camera constraints not supported.'
            };
            updateStatus(msgs[error.name] || `Camera error: ${error.message}`, 'error');
            startBtn.disabled = false;
            if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null; }
        }
    }

    // ─── Challenge cycle ──────────────────────────────────────────────────────
    // FIX 2 – saves a snapshot into activeChallenge so handleValidFrame can still
    // access the nonce after challengeManager.validateChallengeNonce() clears it.
    function startChallengeCycle() {
        // Generate a fresh ephemeral 32-byte session key embedded in the QR code
        const sharedSecret = crypto.getRandomValues(new Uint8Array(32));

        const challenge = challengeManager.generateChallenge(sharedSecret);

        // Keep a local snapshot; this survives the challengeManager clearing it
        activeChallenge = {
            hex:        challenge.hex,
            base64url:  challenge.base64url,
            sharedSecret: sharedSecret   // Uint8Array 32 bytes
        };

        updateQrDisplay(challenge);
        awaitingResponse = true;
        addLogEntry('New challenge generated — scan QR code', 'info');
        resetMinMaxBrightness();

        if (expiryUpdateIntervalId) clearInterval(expiryUpdateIntervalId);
        expiryUpdateIntervalId = setInterval(updateExpiryDisplay, 1000);
    }

    function resetMinMaxBrightness() {
        minBrightness = 255;
        maxBrightness = 0;
        windowBrightnessSamples = [];
    }

    function clearQrDisplay() {
        challengeManager.clearCurrentChallenge();
        qrSection.classList.add('hidden');
        clearCanvasEl(qrCode);
        if (expiryUpdateIntervalId) {
            clearInterval(expiryUpdateIntervalId);
            expiryUpdateIntervalId = null;
        }
    }

    // ─── Stop camera ──────────────────────────────────────────────────────────
    function stopCamera() {
        isStreaming = false;
        awaitingResponse = false;
        activeChallenge = null;

        if (animationFrameId) { cancelAnimationFrame(animationFrameId); animationFrameId = null; }
        if (waveformAnimationId) { cancelAnimationFrame(waveformAnimationId); waveformAnimationId = null; }
        if (expiryUpdateIntervalId) { clearInterval(expiryUpdateIntervalId); expiryUpdateIntervalId = null; }

        cameraPreview.classList.remove('active');
        cameraPlaceholder.classList.remove('hidden');
        waveformCanvas.classList.add('hidden');
        cameraPlaceholder.querySelector('span').textContent = 'Camera off';

        if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null; }

        offscreenCanvas = null;
        offscreenCtx = null;
        resetReceptionState();
        clearQrDisplay();
        clearCredentialDisplay();
        resetMinMaxBrightness();

        updateStatus('Idle', 'info');
        startBtn.disabled = false;
        stopBtn.disabled = true;
        addLogEntry('Scan stopped', 'info');
    }

    // ─── Log toggle ───────────────────────────────────────────────────────────
    function toggleLog() {
        logExpanded = !logExpanded;
        logContent.classList.toggle('collapsed', !logExpanded);
        logToggle.classList.toggle('expanded', logExpanded);
        logToggle.setAttribute('aria-expanded', logExpanded);
    }

    // ─── Event listeners ──────────────────────────────────────────────────────
    startBtn.addEventListener('click', startCamera);
    stopBtn.addEventListener('click', stopCamera);
    logToggle.addEventListener('click', toggleLog);
    window.addEventListener('beforeunload', stopCamera);

    let resizeTimeout;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimeout);
        resizeTimeout = setTimeout(() => { if (waveformCtx) initWaveform(); }, 100);
    });
});
