plugins {
    id(androidx.plugins.application.get().pluginId)
    id(kotlinx.plugins.android.get().pluginId)
    id(kotlinx.plugins.parcelize.get().pluginId)
    alias(libs.plugins.about.libraries)
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.compose.compiler)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase) apply false
}

if (gradle.startParameter.taskRequests.toString().contains("Standard")) {
    apply(mapOf("plugin" to "com.google.gms.google-services"))
    apply(mapOf("plugin" to "com.google.firebase.crashlytics"))
}

val supportedAbis = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

android {
    compileSdk = AndroidConfig.compileSdkVersion
    namespace = "org.nekomanga"

    defaultConfig {
        minSdk = AndroidConfig.minSdkVersion
        targetSdk = AndroidConfig.targetSdkVersion
        applicationId = "org.nekomanga.neko"
        versionCode = 43
        versionName = "3.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        setProperty("archivesBaseName", "Neko")
        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
        buildConfigField("Boolean", "INCLUDE_UPDATER", "false")

        ndk { abiFilters += supportedAbis }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(*supportedAbis.toTypedArray())
            isUniversalApk = true
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["mangadexAuthRedirectUri"] = "mangadex-auth-debug"
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            manifestPlaceholders["mangadexAuthRedirectUri"] = "mangadex-auth"
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
        // Disable some unused things
        aidl = false
        renderScript = false
        shaders = false
        buildConfig = true
    }

    flavorDimensions.add("default")

    productFlavors { create("standard") { buildConfigField("Boolean", "INCLUDE_UPDATER", "true") } }
}

composeCompiler {
    val enableMetrics =
        project.providers.gradleProperty("enableComposeCompilerMetrics").orNull.toBoolean()
    val enableReports =
        project.providers.gradleProperty("enableComposeCompilerReports").orNull.toBoolean()

    val rootProjectDir = rootProject.layout.buildDirectory.asFile.get()
    val relativePath = projectDir.relativeTo(rootDir)
    if (enableMetrics) {
        val buildDirPath = rootProjectDir.resolve("compose-metrics").resolve(relativePath)
        metricsDestination.set(buildDirPath)
    }
    if (enableReports) {
        val buildDirPath = rootProjectDir.resolve("compose-reports").resolve(relativePath)
        reportsDestination.set(buildDirPath)
    }
}

dependencies {
    implementation(projects.constants)
    implementation(projects.core)

    implementation(kotlinx.bundles.kotlin)

    coreLibraryDesugaring(libs.desugaring)

    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.serialization.json)
    implementation(kotlinx.serialization.okio)
    implementation(libs.bundles.ok)
    implementation(libs.tokenbucket)
    implementation(libs.timber)
    implementation(libs.rx.java)
    implementation(libs.tachi.unifile)

    // Modified dependencies
    implementation(libs.j2k.subsample) { exclude(module = "image-decoder") }

    implementation(libs.bundles.tachiyomi)
    implementation(androidx.bundles.androidx)
    implementation(libs.bundles.google)
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    implementation(libs.bundles.rx)
    implementation(libs.bundles.ok)

    // TLS 1.3 support for Android < 10
    implementation(libs.conscrypt)

    implementation(libs.bundles.retrofit)

    // Disk
    implementation(libs.disklrue)

    // HTML parser
    implementation(libs.jsoup)

    // Icons
    implementation(libs.bundles.iconics)
    implementation(libs.simple.icons)

    // Database
    implementation(libs.sqlite)
    implementation(libs.sqlite.android)

    // Dependency injection
    implementation(libs.injekt.core)

    // Image library
    implementation(libs.bundles.coil)

    // Logging
    implementation(libs.timber)

    // Required outsde of version catalog cause aar
    implementation("com.github.inorichi.storio:storio-common:8be19de@aar")
    implementation("com.github.inorichi.storio:storio-sqlite:8be19de@aar")
    implementation("com.mikepenz:community-material-typeface:7.0.96.1-kotlin@aar")
    implementation("com.mikepenz:material-design-icons-dx-typeface:5.0.1.3-kotlin@aar")
    implementation("com.mikepenz:google-material-typeface-outlined:4.0.0.2-kotlin@aar")

    // UI
    implementation(libs.bundles.fastadapter)

    implementation(libs.photoView)
    implementation(libs.cascade)
    implementation(libs.cascade.compose)

    // Compose
    implementation(compose.bundles.compose)
    implementation(compose.gap)
    implementation(compose.bundles.accompanist)
    implementation(compose.number.picker)

    implementation(compose.bundles.charting)

    implementation(compose.swipe)

    implementation(libs.pastelplaceholders)
    implementation(libs.bundles.conductor)
    implementation(libs.versioncompare)
    implementation(libs.tokenbucket)
    implementation(libs.bundles.sandwich)
    implementation(libs.aboutLibraries.compose)
    debugImplementation(libs.leakcanary)

    implementation(libs.bundles.results)

    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk) { exclude(group = "junit", module = "junit") }
}
