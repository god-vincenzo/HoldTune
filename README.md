# HoldTune

HoldTune is an Android-only hobby application (target API 30+, minSdk 26) designed to automatically answer incoming phone calls after a user-defined delay, mute the microphone, and play a selected audio track locally on the user's device. 

---

## 🎵 What HoldTune Does (Scope & Behavior)

In plain language, HoldTune acts as a **private pre-call helper**:
1. **Auto-Answers**: The app answers incoming calls automatically after a configured delay (e.g., 2 seconds).
2. **Mutes Microphone**: Your microphone is immediately muted when the call connects, ensuring the caller hears only silence.
3. **Private Playback**: Selected hold music plays **locally on your device's receiver/speaker** while your microphone is muted. This allows you to listen to a brief track, compose yourself, or verify who is calling.
4. **Tap to Join**: A custom full-screen overlay presents a large green "Answer / Join Call" button. When tapped, the music stops, the microphone is unmuted, and you enter a standard, active call.

> [!IMPORTANT]
> **Audio Transmission Limitation**:
> HoldTune **does NOT transmit hold audio to the caller**. On standard carrier cellular calls (GSM/LTE/5G), the Android operating system isolates the call transmit uplink path for security and privacy. Third-party applications cannot inject audio files into the caller's stream. This app is designed for **private local hold audio only**.

---

## 🔒 Required Permissions & Rationale

To manage call states and display in-call layouts, HoldTune requests the following permissions. Because these are highly sensitive, the app will request them transparently at launch:

*   **Default Dialer / Phone Role**: Required to access system-level calling states (answering, muting, routing audio, and replacing the default calling overlay).
*   **ANSWER_PHONE_CALLS**: Allows the app to programmatically answer ringing incoming calls.
*   **READ_PHONE_STATE**: Allows the app to observe the state of calls (Ringing, Active, Disconnected) and read the incoming phone number.
*   **CALL_PHONE**: Allows the app to place or terminate calls (required to support the red "End Call" button).
*   **READ_CONTACTS**: Allows the app to look up incoming numbers in your contacts database to display the caller's display name instead of a raw number on the overlay.
*   **RECORD_AUDIO**: Required to support the custom in-app voice recorder to capture your own hold tracks via the microphone.
*   **FOREGROUND_SERVICE** & **FOREGROUND_SERVICE_PHONE_CALL**: Allows the Telecom connection to remain active and stable in the background without being terminated mid-call by the operating system.

---

## 📲 APK Installation Instructions

If you have downloaded the pre-compiled `app-release.apk` directly from GitHub Releases, follow these steps to install it:

1.  **Enable Unknown Sources**:
    *   Open your device's **Settings**.
    *   Navigate to **Apps ➔ Special app access ➔ Install unknown apps** (or search "Install unknown apps").
    *   Select the browser or file manager you used to download the APK (e.g., Chrome or Files).
    *   Toggle **Allow from this source** to `On`.
2.  **Install the APK**:
    *   Locate the downloaded `app-release.apk` in your downloads folder.
    *   Tap the file and click **Install**.
3.  **Accept Default Dialer Role**:
    *   Upon first launch, read and accept the **Consent Screen**.
    *   The app will automatically trigger a system dialog asking to set HoldTune as your **Default Phone App**. 
    *   You **MUST** accept this prompt. If HoldTune is not the default dialer, the system will not bind our services, and the auto-answering and local playback features will fail to function.

---

## ⚠️ Known Limitations

*   **Emulator Incompatibilities**: Standard Android emulators do not fully support Telecom framework bindings or audio routes (such as `Usage::VoiceCommunication` routed to the handset receiver). For reliable testing, compile and test the app on a **physical Android device** with a SIM card.
*   **OEM Battery Optimizations**: Device manufacturers like **Samsung, Xiaomi, Huawei, Oppo, and Vivo** enforce aggressive battery savers that terminate background services. If these background services are killed, HoldTune will fail to answer calls. Please follow the instructions in the app's Settings menu to whitelist HoldTune from Battery Optimization ("Unrestricted" battery usage).
*   **No VoIP Support**: HoldTune only intercepts standard cellular PSTN phone calls. It does not auto-answer or play audio for VoIP-based apps (like WhatsApp, Telegram, or Skype).

---

## 🛡️ Security Disclaimer

> [!WARNING]
> **For Personal / Hobby Use Only**
> This application is a personal, open-source hobby project and has **not** undergone professional security audits or compliance reviews. Because the app requires sensitive dialer permissions to manage phone calls, you should **only install APK binaries that you compile yourself or obtain from trusted release channels**. The authors are not responsible for any misuse, call interception compliance violations, or data handling disputes.
