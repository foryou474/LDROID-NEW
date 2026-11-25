# LDROID Release Signing, Installation & Verification Guide

**Application ID:** `com.ldroid.ide`  
**Current Version:** `1.0.0` (`versionCode = 1`)  
**Key Alias Expected by `app/build.gradle.kts`:** `upload`

---

## 1. Secure Release Signing Configuration

`app/build.gradle.kts` reads release signing credentials exclusively from environment variables (never hardcoded or committed):

- `KEYSTORE_PATH` (defaults to `${rootDir}/my-upload-key.jks` if unset)
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- `keyAlias = "upload"`

### Option A — Local Release Signing
1. Generate a private upload keystore outside version control:
   ```bash
   keytool -genkeypair -v \
     -keystore ~/keys/ldroid-upload-key.jks \
     -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Export credentials in your shell session (never commit `.jks` or passwords):
   ```bash
   export KEYSTORE_PATH="$HOME/keys/ldroid-upload-key.jks"
   read -s -p "Store password: " STORE_PASSWORD && echo && export STORE_PASSWORD
   read -s -p "Key password: " KEY_PASSWORD && echo && export KEY_PASSWORD
   ```
3. Build and verify signed release artifacts:
   ```bash
   gradle :app:assembleRelease :app:bundleRelease
   cp app/build/outputs/apk/release/app-release.apk LDROID-release.apk
   cp app/build/outputs/bundle/release/app-release.aab LDROID-release.aab
   apksigner verify --print-certs LDROID-release.apk
   jarsigner -verify -verbose -certs LDROID-release.aab
   sha256sum LDROID-release.apk LDROID-release.aab
   ```

### Option B — GitHub Actions Release Signing (`.github/workflows/android_ci.yml`)
1. Encode your `.jks` file to base64:
   ```bash
   base64 -w 0 ~/keys/ldroid-upload-key.jks
   ```
2. In your GitHub repository **Settings -> Secrets and variables -> Actions**, add:
   - `ANDROID_KEYSTORE_BASE64`
   - `STORE_PASSWORD`
   - `KEY_PASSWORD`
3. Trigger `Android CI` via `workflow_dispatch` or by pushing a release tag (`git tag v1.0.0 && git push origin v1.0.0`).
   The `release_signed` job builds, verifies, checksums, and uploads `LDROID-release.apk` and `LDROID-release.aab`, then deletes the temporary keystore in `if: always()`.

---

## 2. Device Installation & Runtime Verification Checklist (M50 Stage D)

Connect an Android 8.0+ (API 26+) device or emulator with USB debugging enabled and run:

```bash
adb devices -l
adb install -r LDROID-release.apk   # or app/build/outputs/apk/debug/app-debug.apk for debug testing
adb shell am start -n com.ldroid.ide/com.example.MainActivity
```

Verify each area on device and record device model, Android version/API level, and pass/fail:

1. **Startup & Offline Guest Mode:** App launches cleanly with custom LDROID icon and `Offline Guest Mode` active; starter `NeonCounter` workspace initializes.
2. **Primary Navigation & Adaptive Layout:** Switch across **Home, Projects, Editor, AI Studio, Settings** (`NavigationBar` on phone; `NavigationRail` on tablet/foldable). Verify `BackHandler` returns cleanly to Home / closes Developer Lab.
3. **10 Themes (M02 / M22):** In **Settings -> Theme Studio**, preview and apply all 10 themes (including *Cyberpunk Neon*, *Arctic Light*, *Desert Luxe*), verify readable contrast across screens, restart app to verify DataStore persistence, and test Theme JSON Export/Import.
4. **Projects & SAF Import/Export (M04 / M05 / M25):** Create a Compose M3 or Multi-Module workspace, import a single file and a `.zip` project via SAF picker, verify Zip-Slip rejection on malformed entries, and export a project `.zip` backup.
5. **Code Editor, Visual Builder & Live Preview (M06 / M08 / M15):** Edit `MainActivity.kt`, test syntax highlighting, undo/redo, find/replace, encoding/LF toggle, and atomic save. Introduce a syntax error to confirm **Code Live Preview** preserves the last known good preview with a stale warning; fix the error to confirm live refresh. Add components in **Visual Builder** and generate Compose `.kt` & XML layout files.
6. **Terminal & Developer Lab (M11 / M12 / M16 / M17 / M24):** Run `ls -la` and `uname -a` in the integrated terminal, cancel a running command (`sleep 10`), inspect own-PID Logcat, check Memory/CPU profiler, and inspect `LDROID` via **APK/AAB Analyzer** (`Inspect LDROID APK` or SAF `.apk`/`.aab`).
7. **BYOK AI Studio & Git/GitHub (M09 / M10 / M18 / M32):** Verify unconfigured-key warning when no key is set; optionally add a BYOK key in Settings, send a prompt with secret redaction enabled, and confirm proposed file edits require explicit **Approve & Apply Patch**. Inspect a GitHub repository in **Projects -> GitHub & Actions Inspector**.

---

## 3. GitHub Export & Release Delivery Sequence

Because AI Studio manages GitHub authentication outside the agent container:
1. Open AI Studio **Settings / Export -> Push to GitHub** and authorize your repository.
2. Confirm `.github/workflows/android_ci.yml` runs on GitHub Actions (`verify` job).
3. Verify the commit on GitHub, ensure zero secrets or `.jks` files are tracked, and publish a tagged GitHub Release with verified `LDROID-release.apk` / `LDROID-release.aab` and `SHA256SUMS.txt`.
