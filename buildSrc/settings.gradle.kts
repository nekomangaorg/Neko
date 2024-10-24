dependencyResolutionManagement {
    versionCatalogs {
        create("kotlinx") { from(files("../gradle/kotlinx.versions.toml")) }
        create("androidx") { from(files("../gradle/androidx.versions.toml")) }
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
