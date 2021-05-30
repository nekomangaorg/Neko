object Configs {
    const val applicationId = "tachiyomi.mangadex"
    const val compileSdkVersion = 29
    const val minSdkVersion = 24
    const val targetSdkVersion = 29
    const val testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val versionCode = 115
    const val versionName = "2.4.2"
}

object LegacyPluginClassPath {
    const val aboutLibraries = "com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:8.8.6"
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"
    const val googleServices = "com.google.gms:google-services:${Versions.googleServices}"
    const val kotlinExtensions = "org.jetbrains.kotlin:kotlin-android-extensions:${Versions.kotlin}"
    const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinSerializations = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
    const val fireBaseCrashlytics = "com.google.firebase:firebase-crashlytics-gradle:2.3.0"
}

object Plugins {
    const val aboutLibraries = "com.mikepenz.aboutlibraries.plugin"
    const val androidApplication = "com.android.application"
    const val firebaseCrashlytics = "com.google.firebase.crashlytics"
    const val googleServices = "com.google.gms.google-services"
    const val kapt = "kapt"
    const val kotlinParcelize = "kotlin-parcelize"
    const val kotlinAndroid = "android"
    const val kotlinSerialization = "org.jetbrains.kotlin.plugin.serialization"
    const val jetbrainsKotlin = "org.jetbrains.kotlin.android"
    val gradleVersions = PluginClass("com.github.ben-manes.versions", Versions.gradleVersions)
    val kotlinter = PluginClass("org.jmailen.kotlinter", Versions.kotlinter)
}

data class PluginClass(val name: String, val version: String)

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}



