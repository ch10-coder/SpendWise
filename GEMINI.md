# SpendWise Project Guidelines & Workflows

## 📱 Android Build, CI/CD & APK Delivery Workflow

1. **Persistent Keystore & Deterministic Signing**:
   - For all Android CI builds, NEVER rely on the runner's ephemeral default debug key.
   - Ensure a persistent keystore (`app/spendwise.keystore`) is configured in `signingConfigs` for BOTH `debug` and `release` builds in `app/build.gradle.kts`.
   - This guarantees identical SHA-256 certificate fingerprints across all CI builds, enabling seamless in-place APK updates without package conflicts.

2. **Automatic Local APK Delivery on Termux**:
   - Whenever a GitHub Actions release or build completes with a new APK:
     - Automatically download the artifact directly to `/sdcard/Download/SpendWise-v<Version>.apk` via `curl` / `gh release download`.
     - Provide the user with direct 1-tap install instructions pointing to their local Downloads folder.

3. **Signature Mismatch Resolution Protocol**:
   - If the user reports "package conflict with existing package", clearly explain that the previous app on their device was signed with an older key.
   - Instruct the user to perform a one-time full uninstall of the old app before installing the new persistent-keystore APK.
