# Google Play Store Release Preparation for VibeTube

Prepare and verify the existing VibeTube Android application for deployment on the Google Play Store via the Internal Testing track, utilizing Google Play App Signing with upload keys while preserving all existing UI aesthetics, Obsidian Charcoal theme, and features.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following release parameters have been confirmed based on your preferences:

- **Confirmed Signing Method**: Google Play App Signing with an upload key. The Gradle build configuration supports dynamic keystore injection (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) for Play Store upload signing, with fallback verification.
- **Confirmed Release Track**: Google Play Console **Internal testing** track first for private verification and smoke testing before opening closed testing or production rollout.
- **Confirmed Versioning**: Kept at version name `"1.0.0"` and version code `1` in `app/build.gradle.kts`.
- **Preservation Guarantee**: All existing application screens, components, Obsidian Charcoal styling, Creator Studio, Feed, Shorts, AI Assistant (slide-out drawer & chat), Messaging, and Voice/Video calling will remain 100% intact with zero code removal.

---

## 1. Overview & Core Concept

- **What It Does**: VibeTube is a modern social video platform combining dynamic long-form video masterclasses, vertical Shorts, in-app Creator Studio uploads, an intelligent Gemini-powered slide-out AI assistant, and direct messaging with real-time audio/video calls.
- **Target Audience**: Video creators, learners, and community viewers on Android mobile devices (phones, foldables, and tablets).
- **Key Value for Store Release**: Delivering a high-performance, compliant release package (`.aab`) optimized for Google Play distribution, automated dynamic splits, and fast download footprints.

---

## 2. User Experience & Visual Design

- **Visual Identity & Atmosphere**:
  - *Obsidian Charcoal Aesthetic*: Deep dark theme utilizing `#0F0F12` background, elevated `#18181E` surface containers, and vibrant crimson/violet gradients for interactive elements.
  - *Edge-to-Edge Experience*: Standard edge-to-edge window insets padding for system status bars and gesture navigation bars.
  - *Typography & Components*: Clean Material 3 typography with high-contrast text hierarchies, rounded cards (12–16dp), and smooth transitions under 300ms.
- **Store-Facing Presence**:
  - *Launcher Label*: `VibeTube` (aligned in `metadata.json` and `res/values/strings.xml`).
  - *Adaptive Icon*: Customized foreground and background vector adaptive icon set under `res/mipmap-anydpi-v26/ic_launcher.xml`.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Google Play App Signing (AAB) vs Standalone APK**
  - *Chosen Approach*: Android App Bundle (`.aab`) signed with an upload key for Google Play Console.
  - *Why*: Mandatory requirement for all new apps submitted to Google Play. Enables Google Play Feature Delivery, dynamic architecture slicing (arm64-v8a, armeabi-v7a, x86_64), and smaller download sizes.
  - *Alternatives Considered*: Standalone APK (only suitable for direct sideloading or internal testing, rejected for Play Store production).

- **Decision 2: Code Shrinking (R8 / ProGuard)**
  - *Chosen Approach*: Keep `isMinifyEnabled = false` for the initial v1.0.0 release.
  - *Why*: Prevents reflection, serialization, and obfuscation crashes across Room Database, Moshi/Gson, and Coil image loaders without requiring extensive custom Keep rules on first release.
  - *Alternatives Considered*: Aggressive R8 minification (higher risk of runtime missing class exceptions in first launch).

- **Decision 3: SDK Target Alignment**
  - *Chosen Approach*: `targetSdk = 36` (Android 16 / latest compliant requirement) and `minSdk = 24` (Android 7.0+).
  - *Why*: Satisfies current and upcoming Google Play target API level requirements while ensuring 96%+ device compatibility worldwide.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Google Play Console                             │
│       (App Listing, Data Safety, Content Rating, Testing Tracks)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Uploads .aab
┌───────────────────────────────────▼────────────────────────────────────┐
│                  Signed Android App Bundle (.aab)                      │
│                  Target SDK: 36 | Min SDK: 24 | v1.0.0                 │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Built via Gradle
┌───────────────────────────────────▼────────────────────────────────────┐
│                    VibeTube Android Application                        │
│                                                                        │
│  ┌───────────────────────┐  ┌──────────────────┐  ┌─────────────────┐ │
│  │   UI Presentation     │  │   Slide-Out AI   │  │  Direct Chat &  │ │
│  │  (Jetpack Compose M3) │  │ Assistant Drawer │  │    Call Suite   │ │
│  └───────────┬───────────┘  └────────┬─────────┘  └────────┬────────┘ │
│              │                       │                     │          │
│  ┌───────────▼───────────────────────▼─────────────────────▼────────┐ │
│  │                     VibeTubeViewModel                            │ │
│  └───────────────────────────────────┬──────────────────────────────┘ │
│                                      │                                │
│  ┌───────────────────────────────────▼──────────────────────────────┐ │
│  │               Repository & Data Persistence Layer                │ │
│  │      (Room SQLite Database + Local Grounding + Gemini AI)        │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Build & Release Verification Plan

1. **Project Inspection & Verification**:
   - Verify `applicationId`: `com.aistudio.vibetube.qmvxrp`
   - Verify `versionCode = 1`, `versionName = "1.0.0"`
   - Verify `targetSdk = 36`, `minSdk = 24`
   - Verify Manifest declarations (Internet, Camera, Audio Record, launcher activity)
2. **Release Bundle Generation**:
   - Execute `:app:bundleRelease` to produce `app/build/outputs/bundle/release/app-release.aab`.
   - Validate bundle size, integrity, and signing task completion.
3. **Google Play Console Step-by-Step Checklist**:
   - Store Listing metadata (App name, short & full descriptions, category).
   - Content rating questionnaire & Target audience declarations.
   - Data safety declarations (camera/microphone usage for calling, local database storage).
   - Internal test track creation and upload of `app-release.aab`.
