buildscript {
    dependencies {
        classpath(libs.google.services.gradle)
        classpath(libs.firebase.gradle)
        classpath(kotlinx.serialization.gradle)
        classpath(libs.aboutLibraries.gradle)
    }
}

plugins {
    alias(androidx.plugins.application) apply false
    alias(androidx.plugins.library) apply false
    alias(kotlinx.plugins.android) apply false
    alias(libs.plugins.kotlinter)
}


subprojects {
    apply<org.jmailen.gradle.kotlinter.KotlinterPlugin>()

    kotlinter {
        experimentalRules = true
        disabledRules = arrayOf("experimental:argument-list-wrapping")
    }
}


tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
