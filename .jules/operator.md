## 2025-05-15 - Android Build Tools Redundancy
**Learning:** Manual installation of specific Android Build Tools versions (e.g., 29.0.3) in CI workflows is often redundant when using modern Android Gradle Plugin (AGP), which automatically downloads the required version.
**Action:** Audit Android workflows for `sdkmanager "build-tools;..."` steps and remove them if the project uses a recent AGP version, saving setup time.
