# SENSI BOT MAX (iOS Edition) 🎯📱

> **Completely Isolated iOS Project Directory**: `ios/`  
> Zero dependencies on or modifications to the Android codebase (`app/`).

---

## 🏗️ Architecture & Folder Structure

```
ios/
├── Package.swift                             # Swift Package manifest (iOS 15+)
├── README_IOS.md                             # Complete build & ESign installation manual
└── SensiBotMax/
    ├── App/
    │   ├── SensiBotMaxApp.swift             # SwiftUI @main entrypoint & PiP audio configuration
    │   └── Info.plist                       # Permissions, 120Hz ProMotion & PiP background mode
    ├── Core/
    │   ├── IOSSensitivityEngine.swift       # 6-Stage Hardware & Biometric Mathematical Engine
    │   ├── PipOverlayManager.swift          # Picture-in-Picture floating HUD over Free Fire
    │   └── LicenseManager.swift             # Hardware ID locking & Supabase VIP activation
    ├── Theme/
    │   └── ColorTheme.swift                 # Obsidian Black (#07070A) & Ruby Red (#FF2A4D) HUD
    └── Views/
        ├── HomeView.swift                   # Live Hardware Probe & Rapid Sensi Calibrator
        ├── SensiFinderView.swift            # 4-Step Biometric Questionnaire Calibrator
        ├── TouchRateProbeCard.swift         # Real Coalesced Touch Polling Rate (60/120/240Hz)
        ├── SensitivityDisplayCard.swift     # Free Fire 0-200 gauges, Fire Button & HUD badges
        ├── FeedbackTunerCard.swift          # Post-practice self-learning adaptation buttons
        ├── PipOverlayView.swift             # Floating HUD Picture-in-Picture control room
        ├── LicenseView.swift                # Hardware ID copying, key activation & Discord store
        ├── SupportView.swift                # 100% Anti-Ban proof, diagnostics export & Discord link
        └── MainTabView.swift                # Cyberpunk bottom tab bar navigation root
```

---

## 🧠 The 6-Stage iOS Sensitivity Algorithm

### 1. Hardware PPI Lookup (`DeviceProbe`)
Unlike Android's `DisplayMetrics` (`xdpi` / `ydpi`), iOS does not expose physical screen DPI via runtime APIs. The algorithm queries `utsname.machine` against Apple's exact published PPI registry:
* **326 PPI**: iPhone 8, 11, XR, SE 2/3
* **401 PPI**: iPhone 8 Plus
* **458 PPI**: iPhone X, XS, 11 Pro, 12/13/14 Pro Max
* **460 PPI**: iPhone 12, 13, 14, 15, 16 series
* **476 PPI**: iPhone 12 mini, 13 mini

It also queries hardware state:
* `screen.maximumFramesPerSecond` (60Hz vs 120Hz ProMotion)
* `ProcessInfo.isLowPowerModeEnabled`
* `ProcessInfo.thermalState >= .serious` (thermal throttling)
* `screen.scale != screen.nativeScale` (Display Zoom check)

### 2. Real Hardware Coalesced Touch Polling (`TouchRateProbeView`)
Doesn't assume 60 or 120Hz. Measures hardware touch sampling frequency using `event?.coalescedTouches(for: touch)`. Captures high-frequency 120Hz/240Hz touch samples per gesture.

### 3. Stage 1 Baseline Normalization (`IOSBaseline`)
* Reference baseline: 400.0 PPI, 2.54 inch short side.
* Screen travel scaling term: `sizeFactor = min(max(sqrt(referenceWidthIn / p.widthInches), 0.97), 1.03)`.
* Touch sampling adjustment: -3 for ≥110Hz (less jitter headroom required), +3 for ≤65Hz (compensates for 60Hz touch latency).
* Thermal & Low Power compensations: +2 general sensi when throttled.

### 4. Stage 2 Biometric Personal Multiplier (`PersonalMultiplier`)
* Overshoot (-1 chest drag to +1 flying over head)
* Tempo (-1 sniper to +1 rusher)
* Drag consistency (0 erratic to 1 steady)
* Weapon urgency (-1 sniper to +1 shotgun)
* Existing sensitivity baseline delta
* Multiplier range clamped to `[0.80, 1.20]`.

### 5. Final Output & Tactical iOS Advice (`IOSSensitivityEngine`)
* Values clamped to Free Fire's native ranges (General: 100-200, Red Dot: 100-200, 2X: 80-200, 4X: 70-200, Sniper: 50-180, Free Look: 50-200).
* Fire Button percentage: 48-52% (2 fingers) or 44-48% (3-4 fingers).
* iOS-specific advice for Guided Access, Display Zoom, and Low Power Mode.

### 6. Post-Practice Feedback Loop (`FeedbackTuner`)
* Learned bias persists in `UserDefaults` between `-0.10` and `+0.10`.
* Players tap **"Flew Over Head" (+1)**, **"Clean Red" (0)**, or **"Stuck Low" (-1)** after matches to continuously self-tune the engine by `0.015` steps.

---

## 🪟 In-Game Floating HUD (Picture-in-Picture)
iOS does not support Android's `SYSTEM_ALERT_WINDOW` permission. Sensi Bot Max solves this through iOS's native `AVPictureInPictureController`.
1. Launches a video stream renderer displaying live sensitivity values and fire button sizes.
2. The user switches into Free Fire.
3. The HUD stays floating on top in standard iOS Picture-in-Picture mode!

---

## 📦 Building & Installing the `.ipa` (No PC Needed)

### Method A: Direct Install via ESign (On-Device, No PC)
1. Export the compiled `SensiBotMax.ipa`.
2. Send the `.ipa` or download link to the user on **Discord** (`https://discord.gg/6X8fUjeE2D`).
3. User opens **ESign** or **Scarlet** on their iPhone:
   * Tap **Import** -> Select `SensiBotMax.ipa`.
   * Tap **Signature** -> Choose any active free Enterprise Certificate or personal certificate.
   * Tap **Install**.
4. Go to **Settings > General > VPN & Device Management**, tap the certificate profile and tap **Trust**.
5. Done! The app runs fully natively with zero jailbreak.

### Method B: Building with Xcode (Mac / Cloud CI / GitHub Actions)
1. Open the project folder in Xcode:
   ```bash
   cd ios
   open Package.swift # or create standard Xcode project
   ```
2. Select your Apple Developer Account or Personal Free Team.
3. Product -> Archive -> Distribute App -> Ad Hoc / Development.
4. Export as `SensiBotMax.ipa`.
