plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(androidx.gradle)
    implementation(kotlinx.gradle)
    implementation(gradleApi())
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}
