# LightLock

## Project Title

**Local Password Manager and Credential Transfer Using Visible Light**

## Project Overview

LightLock is a local-first password manager that allows users to generate and store credentials on a smartphone and transfer selected credentials to a computer using visible light and a webcam.

The smartphone acts as the credential vault and transmitter, while a browser extension on the computer uses the webcam and computer vision to detect and decode the optical signal. The system is designed to transfer credentials locally without requiring cloud synchronization or a Google account.

## Core Workflow

1. Generate a strong password on the smartphone.
2. Store the credential locally for a specific website or application.
3. When the credential is needed on a computer, select it from the phone.
4. The computer/browser extension generates a one-time challenge.
5. The phone verifies the request and user approval.
6. The phone transmits the credential through visible-light modulation.
7. The webcam captures the changing light.
8. The browser extension uses computer vision to reconstruct and decode the signal.
9. The credential is verified and inserted into the appropriate password field.

## Main Components

### Smartphone App

- Local password vault
- Strong password generator
- Website/application-specific credentials
- Credential selection
- User authorization before transmission
- Visible-light transmitter
- Challenge-response handling

### Browser Extension

- Webcam access
- Optical signal detection
- Region-of-interest detection
- Signal processing and decoding
- Challenge generation
- Integrity verification
- Password-field detection
- Credential autofill

### Computer Vision

The initial prototype can use Morse-style blinking to demonstrate the concept.

The final system can use a binary optical communication protocol for faster and more reliable credential transfer.

Possible processing pipeline:

```text
Webcam
   ↓
Frame Capture
   ↓
Region of Interest
   ↓
Brightness Extraction
   ↓
Noise Filtering
   ↓
Signal Detection
   ↓
Bit Reconstruction
   ↓
Frame Parsing
   ↓
Integrity Verification
   ↓
Credential Decoding
```

## Security Concept

The system should use a fresh challenge for each transfer session to reduce the risk of replaying a previously captured optical transmission.

```text
Computer
   │
   │ One-time challenge
   ▼
Phone
   │
   │ User approval
   │
   │ Credential + challenge
   ▼
Optical transmission
   │
   ▼
Computer
```

The project should be designed as a local-first system. Credentials do not need to be synchronized through a cloud service merely to transfer them between the phone and computer.

## Development Stages

### Phase 1 — Proof of Concept

Implement Morse-code transmission using a phone screen and webcam.

```text
Phone Screen → Webcam → OpenCV → Morse Decoder → Text
```

### Phase 2 — Optical Communication

Replace Morse with a custom binary protocol containing framing, payload, and error detection.

```text
SYNC | LENGTH | PAYLOAD | CHECKSUM | END
```

### Phase 3 — Password Manager

Implement:

- Local credential storage
- Password generation
- Website/application entries
- Credential selection
- Basic vault protection

### Phase 4 — Browser Extension

Implement:

- Webcam capture
- Optical decoding
- Challenge generation
- Password-field detection
- Credential insertion

### Phase 5 — Secure Transfer

Add:

- One-time challenges
- User confirmation
- Integrity verification
- Replay protection
- Secure handling of temporary plaintext credentials

## Technologies

Potential technologies include:

- **Android:** Kotlin
- **Computer Vision:** OpenCV
- **Browser Extension:** JavaScript / TypeScript
- **Browser APIs:** WebExtensions / Chrome Extension APIs
- **Communication:** Visible-light optical modulation
- **Storage:** Encrypted local database
- **Cryptography:** Standard, well-reviewed cryptographic primitives

## Final Concept

**LightLock** combines:

> **Password Management + Computer Vision + Visible-Light Communication + Browser Extension + Security**

The goal is to create a practical local credential-transfer system where a smartphone can securely provide a generated password to a computer without requiring cloud-based credential synchronization.
