plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.google.gms.google-services") apply false
    id("com.google.firebase.crashlytics") apply false

}

if (gradle.startParameter.taskRequests.toString().contains("Standard")) {
    apply(mapOf("plugin" to "com.google.gms.google-services"))
    apply(mapOf("plugin" to "com.google.firebase.crashlytics"))
}

val supportedAbis = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")



android {
    compileSdk = AndroidConfig.compileSdkVersion
    namespace = "eu.kanade.tachiyomi"

    defaultConfig {
        minSdk = AndroidConfig.minSdkVersion
        targetSdk = AndroidConfig.targetSdkVersion
        applicationId = "tachiyomi.mangadex"
        versionCode = 192
        versionName = "2.14.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        setProperty("archivesBaseName", "Neko")
        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
        buildConfigField("Boolean", "INCLUDE_UPDATER", "false")

        ndk {
            abiFilters += supportedAbis
        }
        packaging {
            jniLibs.excludes.addAll(
                listOf(
                    "**/libjxl.so",
                    "**/libjxl_dec.so",
                    "**/libjxl_threads.so",
                ),
            )
        }
        externalNativeBuild {
            cmake {
                this.arguments("-DHAVE_LIBJXL=FALSE")
            }
        }
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = compose.versions.compose.compiler.version.get()
    }

    flavorDimensions.add("default")

    productFlavors {
        create("standard") {
            buildConfigField("Boolean", "INCLUDE_UPDATER", "true")
        }
        create("dev") {

            resourceConfigurations.add("en")
        }
    }

    /* lint {
         disable("MissingTranslation")
         isAbortOnError = false
         isCheckReleaseBuilds = false
     }*/

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation(project(":constants"))
    implementation(project(":core"))
    implementation(kotlinx.bundles.kotlin)

    coreLibraryDesugaring(libs.desugaring)

    // Modified dependencies
    implementation(libs.j2k.subsample) {
        exclude(module = "image-decoder")
    }


    implementation(libs.bundles.tachiyomi)
    implementation(androidx.bundles.androidx)
    implementation(libs.bundles.google)
    implementation(libs.bundles.firebase)
    implementation(libs.bundles.rx)
    implementation(libs.bundles.ok)

    // TLS 1.3 support for Android < 10
    implementation(libs.conscrypt)

    debugImplementation(libs.chucker.debug)
    releaseImplementation(libs.chucker.release)

    implementation(libs.bundles.retrofit)

    // Disk
    implementation(libs.disklrue)

    // HTML parser
    implementation(libs.jsoup)

    // Icons
    implementation(libs.bundles.iconics)
    implementation("br.com.devsrsouza.compose.icons.android:simple-icons:1.0.0")

    //required outside bundle cause toml doesnt work with aar
    implementation("com.mikepenz:community-material-typeface:7.0.96.0-kotlin@aar")
    implementation("com.mikepenz:material-design-icons-dx-typeface:5.0.1.2-kotlin@aar")
    implementation("com.mikepenz:google-material-typeface-outlined:4.0.0.1-kotlin@aar")

    // Database
    implementation("androidx.sqlite:sqlite:2.3.1")
    implementation("com.github.inorichi.storio:storio-common:8be19de@aar")
    implementation("com.github.inorichi.storio:storio-sqlite:8be19de@aar")
    implementation("com.github.requery:sqlite-android:3.39.2")

    // Model View Presenter
    implementation(libs.bundles.nucleus)

    // Dependency injection
    implementation("com.github.inorichi.injekt:injekt-core:65b0440")

    // Image library
    implementation(libs.bundles.coil)

    // Logging
    implementation(libs.timber)

    // UI
    implementation(libs.bundles.fastadapter)
    implementation("com.github.leandroBorgesFerreira:LoadingButtonAndroid:2.2.0")


    implementation(libs.bundles.flexibleadapter)

    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.CarlosEsco:ViewTooltip:f79a8955ef")
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")
    implementation("me.saket.cascade:cascade:2.3.0")
    implementation("me.saket.cascade:cascade-compose:2.3.0")

    //Compose
    implementation(compose.bundles.compose)
    implementation(compose.gap)
    implementation(compose.bundles.accompanist)
    implementation(compose.number.picker)

    implementation(compose.bundles.charting)


    implementation(libs.pastelplaceholders)
    implementation(libs.bundles.conductor)
    implementation(libs.fuzzysearch)
    implementation(libs.versioncompare)
    implementation(libs.tokenbucket)
    implementation(libs.bundles.kahelpers)
    implementation(libs.sandwich)
    implementation(libs.aboutLibraries.compose)
    debugImplementation(libs.leakcanary)

    implementation(libs.bundles.results)

    testImplementation(libs.bundles.tests)

}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xcontext-receivers",
            "-opt-in=kotlin.Experimental",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=coil.annotation.ExperimentalCoilApi",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.InternalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )

        /*kotlinOptions.freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.buildDir.absolutePath + "/compose_metrics",
        )

        kotlinOptions.freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + project.buildDir.absolutePath + "/compose_metrics",
        )*/

    }

    preBuild {
        // dependsOn(formatKotlin)
    }
}

