# LDROID — Android Development IDE

**Package ID:** `com.ldroid.ide`  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3)  
**SDK Baseline:** `compileSdk 36`, `targetSdk 36`, `minSdk 24` (compatible with Android 8.0+ / API 26+ baseline)

## Implemented Capabilities
- **10 Complete Design Themes (M02 / M22):** Cyberpunk Neon (default), Obsidian Glass, Midnight Minimal, Royal Purple, Emerald Matrix, Arctic Light, Royal Sapphire, Crimson Inferno, Desert Luxe, and Holographic X — with instant preview, Apply/Cancel, DataStore persistence, WCAG AA contrast validation, and JSON import/export.
- **Offline-First Guest Workspace & SAF File Management (M04 / M05 / M25):** Create multi-file projects from templates (Compose M3 App, Multi-Module Android Workspace, Web Sandbox), import files or ZIP archives safely with Zip-Slip path traversal protection via Storage Access Framework, browse ZIPs/images, and export ZIP backups.
- **Code Editor & Visual Builder (M06 / M08 / M15):** Line numbers, syntax highlighting, find/replace, undo/redo, encoding/LF/CRLF selector, atomic saves, large-file streaming preview guard, Visual UI component builder generating real Kotlin Compose & Android XML, plus Code Live Preview & Visual UI Live Preview with device frames.
- **Integrated Terminal & Developer Lab (M11 / M12 / M16 / M17 / M24):** Real `ProcessBuilder` shell sessions with cancel support, own-PID Logcat viewer, live JVM/native memory telemetry, on-device static & secret analyzer, and a real APK/AAB/ZIP analyzer (ZIP buckets, DEX versions, PackageManager manifest/permissions, X.509 signing cert SHA-256).
- **BYOK AI Studio & Git/GitHub Inspector (M09 / M10 / M18 / M32):** AndroidKeyStore AES-256-GCM encrypted API keys (Gemini, OpenAI-compatible, Anthropic), secret redaction before dispatch, reviewable multi-file diff patches requiring explicit approval, local baseline unified diffing, and GitHub REST repository/workflow inspection.

## Build, Lint & Verification
```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
gradle :app:assembleDebug
```

See [docs/RELEASE_AND_SIGNING.md](docs/RELEASE_AND_SIGNING.md) for release keystore configuration, GitHub Actions signed `LDROID-release.apk` / `LDROID-release.aab` generation, and the M50 device runtime verification checklist.
