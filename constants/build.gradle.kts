plugins {
    id(androidx.plugins.library.get().pluginId)
    id(kotlinx.plugins.android.get().pluginId)
}

android { namespace = "org.nekomanga.constants" }

dependencies {
    implementation(androidx.core.ktx)
}
