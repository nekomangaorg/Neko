plugins {
    id(Plugins.ktLint.name) version Plugins.ktLint.version
    id(Plugins.gradleVersions.name) version Plugins.gradleVersions.version
}
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
        jcenter()
    }
}

subprojects {
    apply(plugin = Plugins.ktLint.name)
    ktlint {
        debug.set(true)
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        ignoreFailures.set(true)
        enableExperimentalRules.set(false)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.JSON)
        }
        filter {
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }
}

buildscript {
    dependencies {
        classpath(LegacyPluginClassPath.fireBaseCrashlytics)
        classpath(LegacyPluginClassPath.androidGradlePlugin)
        classpath(LegacyPluginClassPath.googleServices)
        classpath(LegacyPluginClassPath.kotlinExtensions)
        classpath(LegacyPluginClassPath.kotlinPlugin)
        classpath(LegacyPluginClassPath.aboutLibraries)
        classpath(LegacyPluginClassPath.kotlinSerializations)
    }
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
    }
}

tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java).configure {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
    // optional parameters
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
