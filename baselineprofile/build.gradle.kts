plugins {
    id("com.android.test")
    id(androidx.plugins.baselineprofile.get().pluginId)
}

android {
    namespace = "org.nekomanga.baselineprofile"
    compileSdk = AndroidConfig.compileSdkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = AndroidConfig.minSdkVersion
        targetSdk = AndroidConfig.targetSdkVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    testOptions {
        // Force test results to stay in the console
        animationsDisabled = true
    }
}

dependencies {
    implementation(androidx.uiautomator)
    implementation(androidx.benchmark.macro)
    implementation(androidx.test.ext.junit)

    implementation(project(":app"))
}

baselineProfile {
    useConnectedDevices = true
}
