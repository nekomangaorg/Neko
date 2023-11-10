buildscript {
    dependencies {
        classpath(libs.google.services.gradle)
        classpath(libs.firebase.gradle)
        classpath(kotlinx.serialization.gradle)
        classpath(libs.aboutLibraries.gradle)
    }
}

plugins {
    id(androidx.plugins.application.get().pluginId) apply false
    id(androidx.plugins.library.get().pluginId) apply false
    id(kotlinx.plugins.android.get().pluginId) apply false
    alias(libs.plugins.kotlinter)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
