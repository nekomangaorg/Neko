plugins {
    id("org.jmailen.kotlinter") version "3.7.0"
    id("org.jetbrains.kotlin.android") version "1.6.0" apply false
}
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }
}

subprojects {
    apply(plugin = "org.jmailen.kotlinter")

    kotlinter {
        experimentalRules = true
        disabledRules = arrayOf("experimental:argument-list-wrapping")
    }
}

buildscript {
    dependencies {
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.8.0")
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath("com.google.gms:google-services:4.3.10")
        val kotlinVersion = "1.5.31"
        classpath("org.jetbrains.kotlin:kotlin-android-extensions:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:8.9.4")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    }
    repositories {
        gradlePluginPortal()
        google()
    }
}


tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
