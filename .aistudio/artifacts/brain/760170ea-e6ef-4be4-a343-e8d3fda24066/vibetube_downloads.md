# VibeTube Android Project Download Center

Your complete VibeTube Android project and verified installable APK files are prepared and verified.

---

## 📦 Ready-to-Download Files

| File | Type | Size | Location |
| :--- | :--- | :--- | :--- |
| **`vibetube-source.zip`** | Full Project Source ZIP | **807 KB** | `./vibetube-source.zip` |
| **`app-debug.apk`** | Installable Android APK | **24 MB** | `./app-debug.apk` |
| **`app-release.apk`** | Signed Release APK | **17 MB** | `app/build/outputs/apk/release/app-release.apk` |

---

## 📥 How to Download from the AI Studio UI

You can download your files using either of the following methods in the AI Studio interface:

### Method 1: Download from the Left Files Panel (Direct Download)
1. In the **Files** explorer on the left sidebar:
   - Click on **`vibetube-source.zip`** to download the complete source code archive.
   - Click on **`app-debug.apk`** to download the installable Android package.
2. If using the three-dot menu next to the file name, select **Download**.

### Method 2: Export Entire Project Button
1. In the top-right corner of Google AI Studio, click on the **Settings / More Options** menu (three dots or gear icon).
2. Click **Export Project** to save a complete `.zip` of all workspace files directly to your device.

---

## 🛠️ Project Contents in `vibetube-source.zip`

The ZIP package contains the complete Android Studio project:
- **Build Configurations**: `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/`
- **Application Manifest**: `app/src/main/AndroidManifest.xml` (Camera, Microphone, Internet permissions)
- **UI & Presentation**:
  - `WatchScreen.kt` & `VideoPlayerView.kt` (Full video player with timeline, comments, like, and share)
  - `ExploreScreen.kt` (Video & creator search with category filtering)
  - `ShortsScreen.kt` (Vertical Shorts swipe player)
  - `AiAssistantSlideOutDrawer.kt` (Slide-out AI co-pilot in messages)
  - `MessagesInboxScreen.kt` & `ChatDetailScreen.kt` (Direct chat & group messaging)
  - `ActiveCallScreen.kt` (Live voice and video calling interface)
  - `Theme.kt`, `Color.kt`, `Type.kt` (Obsidian Charcoal Material 3 dark theme)
- **Data & AI Layer**:
  - `VibeTubeDatabase.kt` & `VibeTubeDao.kt` (Room SQLite database)
  - `GeminiClient.kt` (Gemini API with grounded video recommendations and tone rewriter)
  - `VibeTubeViewModel.kt` (Complete state management)

---

## 📱 Installing `app-debug.apk` on Android

1. Transfer `app-debug.apk` to any Android device running **Android 7.0 (API 24) or higher**.
2. Open your device's **Files** or **Downloads** app and tap `app-debug.apk`.
3. Tap **Install** (if prompted, enable *"Install unknown apps"* for your browser or file manager).
4. Tap **Open** to launch VibeTube!
