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
        managedDevices {
            localDevices {
                create("pixel6Api31") {
                    device = "Pixel 6"
                    apiLevel = 31
                    systemImageSource = "aosp"
                    testedAbi = "x86_64"
                }
            }
        }
    }
}

dependencies {
    implementation(androidx.uiautomator)
    implementation(androidx.benchmark.macro)
    implementation(androidx.test.ext.junit)
}

baselineProfile {
    useConnectedDevices = true
    managedDevices.add("pixel6Api31")
}
