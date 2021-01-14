object Configs {
    const val applicationId = "tachiyomi.mangadex"
    const val buildToolsVersion = "29.0.3"
    const val compileSdkVersion = 29
    const val minSdkVersion = 24
    const val targetSdkVersion = 29
    const val testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val versionCode = 101
    const val versionName = "2.1.5.3"
}

object LegacyPluginClassPath {
    const val aboutLibraries = "com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:${Versions.aboutLibraries}"
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
    const val kotlinAndroid = "android"
    const val kotlinExtensions = "android.extensions"
    const val kotlinSerialization = "org.jetbrains.kotlin.plugin.serialization"
    val gradleVersions = PluginClass("com.github.ben-manes.versions", Versions.gradleVersions)
    val ktLint = PluginClass("org.jlleitschuh.gradle.ktlint", Versions.ktlint)
}

data class PluginClass(val name: String, val version: String)

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}



