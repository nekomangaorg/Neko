import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object Configs {
    const val applicationId = "tachiyomi.mangadex"
    const val compileSdkVersion = 31
    const val minSdkVersion = 24
    const val targetSdkVersion = 30
    const val testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val versionCode = 138
    const val versionName = "2.7.3.2"
}

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services") apply false
}

fun getBuildTime() = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now(ZoneOffset.UTC))
fun getCommitCount() = runCommand("git rev-list --count HEAD")
fun getGitSha() = runCommand("git rev-parse --short HEAD")

fun runCommand(command: String): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split(" ")
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

android {
    compileSdk = Configs.compileSdkVersion

    defaultConfig {
        minSdk = Configs.minSdkVersion
        targetSdk = Configs.targetSdkVersion
        applicationId = Configs.applicationId
        versionCode = Configs.versionCode
        versionName = Configs.versionName
        testInstrumentationRunner = Configs.testInstrumentationRunner
        multiDexEnabled = true
        setProperty("archivesBaseName", "Neko")
        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
        buildConfigField("Boolean", "INCLUDE_UPDATER", "false")

        ndk {
            abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86")
        }

    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.0.5"
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

    lint {
        disable("MissingTranslation")
        isAbortOnError = false
        isCheckReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    // Modified dependencies
    implementation("com.github.jays2kings:subsampling-scale-image-view:dfd3e43") {
        exclude(module = "image-decoder")
    }

    implementation("com.github.tachiyomiorg:image-decoder:0e91111")
    //implementation("com.github.tachiyomiorg:image-decoder:7481a4a")

    // Source models and interfaces from Tachiyomi 1.x
    implementation("tachiyomi.sourceapi:source-api:1.1")

    // Android X libraries
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.5.0-beta01")
    implementation("androidx.webkit:webkit:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.preference:preference:1.1.1")
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("com.google.firebase:firebase-analytics-ktx:20.0.2")
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.2.6")

    val lifecycleVersion = "2.4.0"
    kapt("androidx.lifecycle:lifecycle-compiler:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // ReactiveX
    implementation("io.reactivex:rxandroid:1.2.1")
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("com.jakewharton.rxrelay:rxrelay:1.2.0")
    implementation("com.github.pwittchen:reactivenetwork:0.13.0")

    // Coroutines
    implementation("com.github.tfcporciuncula:flow-preferences:1.3.4")

    // Network client
    val okhttpVersion = "4.9.3"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:$okhttpVersion")
    implementation("com.squareup.okio:okio:2.10.0")

    // TLS 1.3 support for Android < 10
    implementation("org.conscrypt:conscrypt-android:2.5.2")

    // Chucker
    val chuckerVersion = "3.5.2"
    debugImplementation("com.github.ChuckerTeam.Chucker:library:$chuckerVersion")
    releaseImplementation("com.github.ChuckerTeam.Chucker:library-no-op:$chuckerVersion")

    // REST
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")

    // JSON
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    // Serialization
    val kotlinSerialization = "1.3.2"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlinSerialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${kotlinSerialization}")

    // Disk
    implementation("com.jakewharton:disklrucache:2.0.2")
    implementation("com.github.tachiyomiorg:unifile:17bec43")

    // HTML parser
    implementation("org.jsoup:jsoup:1.14.3")

    // Icons
    implementation("com.mikepenz:iconics-core:5.3.3")
    implementation("com.mikepenz:iconics-views:5.3.3")
    implementation("com.mikepenz:community-material-typeface:6.4.95.0-kotlin@aar")
    implementation("com.mikepenz:material-design-icons-dx-typeface:5.0.1.2-kotlin@aar")
    implementation("com.mikepenz:google-material-typeface-outlined:4.0.0.1-kotlin@aar")

    // Job scheduling
    implementation("androidx.work:work-runtime-ktx:2.7.1")

    implementation("com.google.android.gms:play-services-gcm:17.0.0")

    // Database
    implementation("androidx.sqlite:sqlite:2.2.0")
    implementation("com.github.inorichi.storio:storio-common:8be19de@aar")
    implementation("com.github.inorichi.storio:storio-sqlite:8be19de@aar")
    implementation("io.requery:sqlite-android:3.31.0")

    // Model View Presenter
    val nucleusVersion = "3.0.0"
    implementation("info.android15.nucleus:nucleus:$nucleusVersion")
    implementation("info.android15.nucleus:nucleus-support-v7:$nucleusVersion")

    // Dependency injection
    implementation("com.github.inorichi.injekt:injekt-core:65b0440")

    // Image library
    val coilVersion = "1.4.0"
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")
    implementation("io.coil-kt:coil-svg:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")

    // Logging
    implementation("com.elvishew:xlog:1.11.0")

    // UI
    implementation("com.dmitrymalkovich.android:material-design-dimens:1.4")
    implementation("br.com.simplepass:loading-button-android:2.2.0")
    val fastAdapterVersion = "5.6.0"
    implementation("com.mikepenz:fastadapter:$fastAdapterVersion")
    implementation("com.mikepenz:fastadapter-extensions-binding:$fastAdapterVersion")
    implementation("eu.davidea:flexible-adapter:5.1.0")
    implementation("eu.davidea:flexible-adapter-ui:1.0.0")
    implementation("com.nononsenseapps:filepicker:2.5.2")
    implementation("com.nightlynexus.viewstatepageradapter:viewstatepageradapter:1.1.0")
    implementation("com.github.sephiroth74:NumberSlidingPicker:1.0.3")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.tachiyomiorg:DirectionalViewPager:1.0.0")
    implementation("com.github.florent37:viewtooltip:1.2.2")
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")
    implementation("me.saket.cascade:cascade:1.3.0")

    //Compose
    val composeVersion = "1.0.5"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.runtime:runtime-livedata:$composeVersion")
    // Tooling support (Previews, etc.)
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    // Foundation (Border, Background, Box, Image, Scroll, shapes, animations, etc.)
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    // Material Design
    implementation("androidx.compose.material:material:$composeVersion")
    // Material design icons
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("com.google.android.material:compose-theme-adapter:$composeVersion")

    implementation("com.github.zedlabs:pastelPlaceholders:1.0.4")

    //accompianist
    val accompanistVersion = "0.20.3"
    implementation("com.google.accompanist:accompanist-swiperefresh:$accompanistVersion")

// Conductor
    implementation("com.bluelinelabs:conductor:3.1.2")
    implementation("com.github.tachiyomiorg:conductor-support-preference:3.0.0")

// RxBindings
    val rxBindingVersion = "1.0.1"
    implementation("com.jakewharton.rxbinding:rxbinding-kotlin:$rxBindingVersion")
    implementation("com.jakewharton.rxbinding:rxbinding-appcompat-v7-kotlin:$rxBindingVersion")
    implementation("com.jakewharton.rxbinding:rxbinding-support-v4-kotlin:$rxBindingVersion")
    implementation("com.jakewharton.rxbinding:rxbinding-recyclerview-v7-kotlin:$rxBindingVersion")

// Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("org.mockito:mockito-core:4.3.0")

    val roboElectricVersion = "3.3.2"
    testImplementation("org.robolectric:robolectric:$roboElectricVersion")
    testImplementation("org.robolectric:shadows-multidex:$roboElectricVersion")
    testImplementation("org.robolectric:shadows-play-services:$roboElectricVersion")

    implementation(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
    implementation(kotlin("reflect", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

// Text distance
    implementation("info.debatty:java-string-similarity:2.0.0")

//  version comparison
    implementation("com.g00fy2:versioncompare:1.3.4")

// token bucket
    implementation("org.isomorphism:token-bucket:1.7")
// needed to compile with token bucket
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")

//helpers
    val kahelpersVersion = "3.0.5"
    implementation("com.github.FunkyMuse.KAHelpers:kotlinextensions:$kahelpersVersion")
    implementation("com.github.FunkyMuse.KAHelpers:activity:$kahelpersVersion")
    implementation("com.github.skydoves:sandwich:1.2.3")

    implementation("com.mikepenz:aboutlibraries:8.9.4")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.8.1")

}

tasks {
// See https://kotlinlang.org/docs/reference/experimental.html#experimental-status-of-experimental-api(-markers)
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.Experimental",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-Xopt-in=coil.annotation.ExperimentalCoilApi",
            "-Xuse-experimental=kotlin.ExperimentalStdlibApi",
            "-Xuse-experimental=kotlinx.coroutines.FlowPreview",
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlinx.coroutines.InternalCoroutinesApi",
            "-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    preBuild {
// dependsOn(formatKotlin)
    }
}

if (gradle.startParameter.taskRequests.toString().contains("Standard")) {
    apply(mapOf("plugin" to "com.google.gms.google-services"))
}
