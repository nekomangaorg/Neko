plugins {
    id(androidx.plugins.library.get().pluginId)
    id(kotlinx.plugins.android.get().pluginId)
}

android { namespace = "org.nekomanga.constants"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(androidx.core.ktx)
}
