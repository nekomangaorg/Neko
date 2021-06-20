import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    id(Plugins.androidApplication)
    kotlin(Plugins.kotlinAndroid)
    kotlin(Plugins.kapt)
    id(Plugins.kotlinParcelize)
    id(Plugins.kotlinSerialization)
    id(Plugins.aboutLibraries)
    id(Plugins.firebaseCrashlytics)
    id(Plugins.googleServices) apply false
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
    compileSdkVersion(Configs.compileSdkVersion)

    defaultConfig {
        minSdkVersion(Configs.minSdkVersion)
        targetSdkVersion(Configs.targetSdkVersion)
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

        buildFeatures {
            dataBinding = true
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        viewBinding = true
    }

    flavorDimensions("default")

    productFlavors {
        create("standard") {
            buildConfigField("Boolean", "INCLUDE_UPDATER", "true")
        }
        create("dev") {
            resConfig("en")
        }
    }

    lintOptions {
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
    implementation(Libs.UI.subsamplingScaleImageView)
    // Source models and interfaces from Tachiyomi 1.x
    implementation("tachiyomi.sourceapi:source-api:1.1")
    // Android support library
    implementation(Libs.Android.appCompat)
    implementation(Libs.Android.cardView)
    implementation(Libs.Android.material)
    implementation(Libs.Android.recyclerView)
    implementation(Libs.Android.preference)
    implementation(Libs.Android.annotations)
    implementation(Libs.Android.browser)
    implementation(Libs.Android.biometric)
    implementation(Libs.Android.palette)
    implementation(Libs.Android.coreKtx)
    implementation(Libs.Android.constraintLayout)
    implementation(Libs.Android.multiDex)

    // Databinding for autocomplete search
    kapt(Libs.Android.dataBinding)

    implementation(Libs.Google.firebaseAnayltics)
    implementation(Libs.Google.firebaseCrashltyics)
    implementation(Libs.Google.firebaseCore)

    implementation(Libs.Android.lifecycleExtensions)
    implementation(Libs.Android.lifecycleCommonJava8)
    implementation(Libs.Android.lifecycleRuntimeKtx)

    // ReactiveX
    implementation(Libs.Rx.android)
    implementation(Libs.Rx.java)
    implementation(Libs.Rx.relay)
    implementation(Libs.Rx.preferences)
    implementation(Libs.Rx.network)

    // Coroutines
    implementation(Libs.Kotlin.flowPreferences)

    // Network client
    implementation(Libs.Network.okhttp)
    implementation(Libs.Network.okhttpDns)
    implementation(Libs.Network.okhttpLoggingInterceptor)
    implementation(Libs.IO.okio)

    // TLS 1.3 support for Android < 10
    implementation("org.conscrypt:conscrypt-android:2.5.2")

    // Chucker
    debugImplementation(Libs.Network.chucker)
    releaseImplementation(Libs.Network.chuckerNoOp)

    // hyperion
    debugImplementation(Libs.Hyperion.attr)
    debugImplementation(Libs.Hyperion.buildConfig)
    debugImplementation(Libs.Hyperion.core)
    debugImplementation(Libs.Hyperion.crash)
    debugImplementation(Libs.Hyperion.disk)
    debugImplementation(Libs.Hyperion.geigerCounter)
    debugImplementation(Libs.Hyperion.measurement)
    debugImplementation(Libs.Hyperion.phoenix)
    debugImplementation(Libs.Hyperion.recorder)
    debugImplementation(Libs.Hyperion.sharedPreferences)
    debugImplementation(Libs.Hyperion.timber)

    // REST
    implementation(Libs.Network.retrofit)
    implementation(Libs.Network.retrofitGsonConverter)
    implementation(Libs.Network.retrofitKotlinxConverter)

    // JSON
    implementation(Libs.IO.gson)
    implementation(Libs.IO.kotson)
    implementation(Libs.IO.moshi)

    // Serialization
    implementation(Libs.Kotlin.serialization)
    implementation(Libs.Kotlin.serializationProtobuf)

    // Disk
    implementation(Libs.Disk.lrucache)
    implementation(Libs.Disk.unifile)

    // HTML parser
    implementation(Libs.Parsing.jsoup)

    // Icons
    implementation("com.mikepenz:iconics-core:5.0.3")
    implementation("com.mikepenz:iconics-views:5.0.3")
    implementation("com.mikepenz:community-material-typeface:5.0.45.1-kotlin@aar")
    implementation("com.mikepenz:material-design-icons-dx-typeface:5.0.1.0-kotlin@aar")

    // Job scheduling
    implementation(Libs.Android.workManager)
    implementation(Libs.Android.workManagerKtx)
    implementation(Libs.Google.playServices)

    // Changelog
    implementation(Libs.Util.changelog)

    // Database
    implementation(Libs.Database.sqlite)
    implementation(Libs.Database.storioCommon)
    implementation(Libs.Database.storioSqlite)
    implementation(Libs.Database.requerySqlite)

    // Model View Presenter
    implementation(Libs.Navigation.nucleus)
    implementation(Libs.Navigation.nucleusSupport)

    // Dependency injection
    implementation(Libs.Util.injekt)

    // Image library
    implementation(Libs.Image.coil)
    implementation(Libs.Image.coilGif)
    implementation(Libs.Image.coilSvg)

    // Logging
    implementation(Libs.Util.xlog)

    // UI
    implementation(Libs.UI.materalDesignDimens)
    implementation(Libs.UI.loadingButton)
    implementation(Libs.UI.fastAdapter)
    implementation(Libs.UI.fastAdapterBinding)
    implementation(Libs.UI.flexibleAdapter)
    implementation(Libs.UI.flexibleAdapterUi)
    implementation(Libs.UI.filePicker)
    implementation(Libs.UI.materialDialogsCore)
    implementation(Libs.UI.materialDialogsInput)
    implementation(Libs.UI.materialDateTime)
    implementation(Libs.UI.systemUiHelper)
    implementation(Libs.UI.viewStatePager)
    implementation(Libs.UI.numberSlidingPicker)

    implementation(Libs.UI.androidTagGroup)
    implementation(Libs.UI.photoView)
    implementation(Libs.UI.directionalPageView)
    implementation(Libs.UI.viewToolTip)
    implementation(Libs.UI.tapTargetView)
    implementation(Libs.UI.cascade)

    // Conductor
    implementation(Libs.Navigation.conductor)
    implementation(Libs.Navigation.conductorSupportPreferences)

    // RxBindings
    implementation(Libs.Rx.bindingAppcompat)
    implementation(Libs.Rx.bindingKotlin)
    implementation(Libs.Rx.bindingSupport)
    implementation(Libs.Rx.bindingRecycler)

    // Tests
    testImplementation(Libs.Test.junit4)
    testImplementation(Libs.Test.assertJCore)
    testImplementation(Libs.Test.mockito)

    testImplementation(Libs.Test.roboElectric)
    testImplementation(Libs.Test.roboElectricMultidex)
    testImplementation(Libs.Test.roboElectricShadowPlayServices)

    implementation(Libs.Kotlin.stdLib)
    implementation(Libs.Kotlin.reflection)
    implementation(Libs.Kotlin.coroutines)

    // Text distance
    implementation(Libs.Util.stringSimilarity)

    //  version comparison
    implementation(Libs.Util.versionCompare)

    // token bucket
    implementation(Libs.Util.tokenBucket)
    // needed to compile with token bucket
    implementation(Libs.Util.listenableFutureConflictResolve)

    implementation(Libs.Util.aboutLibraries)

}

tasks {
    // See https://kotlinlang.org/docs/reference/experimental.html#experimental-status-of-experimental-api(-markers)
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.Experimental",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xuse-experimental=kotlin.ExperimentalStdlibApi",
            "-Xuse-experimental=kotlinx.coroutines.FlowPreview",
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlinx.coroutines.InternalCoroutinesApi",
            "-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    preBuild {
        // dependsOn(formatKotlin)
    }
}

if (gradle.startParameter.taskRequests.toString().contains("Standard")) {
    apply(mapOf("plugin" to "com.google.gms.google-services"))
}
