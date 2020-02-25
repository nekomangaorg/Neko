// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.github.ben-manes.versions") version "0.28.0"
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.3")
        classpath("com.google.gms:google-services:4.3.3")
        classpath("org.jmailen.gradle:kotlinter-gradle:2.3.1")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        maven { setUrl("https://www.jitpack.io") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
        jcenter()
    }
}