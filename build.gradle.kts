// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins{
    id("com.github.ben-manes.versions") version "0.28.0"
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        jcenter()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}