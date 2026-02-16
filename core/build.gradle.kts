plugins {
    id(androidx.plugins.library.get().pluginId)
    id(kotlinx.plugins.android.get().pluginId)
    alias(kotlinx.plugins.serialization)
}

android {
    namespace = "org.nekomanga.core"
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(projects.constants)
    implementation(androidx.preferencektx)
    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.serialization.json)
    implementation(kotlinx.serialization.okio)
    implementation(libs.bundles.ok)
    implementation(libs.tokenbucket)
    implementation(libs.timber)
    implementation(libs.jsoup)
    implementation(libs.tachi.unifile)
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
}
