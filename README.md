# BT Popup (Phase 1)

AirPods-style popup for Android: when a Bluetooth audio device connects, a card slides up over
whatever app is open, with the device slowly rotating. Nothing Headphone (1) uses your photos;
other devices get a spinning headphone icon until you add their photos.

## Build with GitHub Actions
1. Create a new GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab, run **Build APK** (or push to `main`).
3. Download the `bt-popup-debug-apk` artifact, unzip, install the APK on the phone.

## First run
1. Grant Bluetooth permission.
2. Grant "Display over other apps".
3. Turn on "Watch for connections".
4. Use **Preview popup** to see the animation without connecting anything, then connect your headphones.

## Next phases
- Battery level (best effort; some devices don't report it)
- "Nearby, tap to connect" popup via BLE scan
- Per-device photos picked from the gallery
