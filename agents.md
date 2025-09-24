# How to run this app

This document provides instructions on how to build and run the Neko manga reader application.

## Prerequisites

- Android SDK installed and configured.
- An Android device or emulator running.

## 1. Set up the environment

If you haven't already, you need to set up the Android SDK.

First, create a `local.properties` file in the root of the project with the following content:

```
sdk.dir=/path/to/your/android/sdk
```

Replace `/path/to/your/android/sdk` with the actual path to your Android SDK installation. For example, in the dev environment, the path is `/home/jules/Android/sdk`.

## 2. Build the debug APK

Open a terminal in the root of the project and run the following command to build the debug APK:

```bash
./gradlew :app:assembleDebug
```

This will download all the required dependencies and build the app. The final APK will be located at `app/build/outputs/apk/standard/debug/app-standard-debug.apk`.

## 3. Install the APK

Use the Android Debug Bridge (adb) to install the APK on your device or emulator:

```bash
adb install app/build/outputs/apk/standard/debug/app-standard-debug.apk
```

## 4. Run the app

Finally, use the following adb command to start the main activity of the app:

```bash
adb shell am start -n org.nekomanga.neko.debug/eu.kanade.tachiyomi.ui.main.MainActivity
```

The app should now be running on your device or emulator.
