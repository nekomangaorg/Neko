import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(androidx.plugins.application.get().pluginId) apply false
    id(androidx.plugins.library.get().pluginId) apply false
    alias(libs.plugins.google.services) apply false
    id(kotlinx.plugins.android.get().pluginId) apply false
    alias(kotlinx.plugins.compose.compiler) apply false
    id(kotlinx.plugins.jvm.get().pluginId) apply false
    id(kotlinx.plugins.parcelize.get().pluginId) apply false
    alias(libs.plugins.about.libraries) apply false
    alias(kotlinx.plugins.serialization) apply false
    alias(libs.plugins.firebase) apply false
    alias(libs.plugins.ktfmt)
}

subprojects {
    tasks {
        withType<KotlinCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    listOf(
                        "-Xcontext-parameters",
                        "-opt-in=kotlin.Experimental",
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                        "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
                        "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
                        "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
                        "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                        "-opt-in=kotlin.time.ExperimentalTime",
                        "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
                        "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                        "-opt-in=coil.annotation.ExperimentalCoilApi",
                        "-opt-in=kotlin.ExperimentalStdlibApi",
                        "-opt-in=kotlinx.coroutines.FlowPreview",
                        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                        "-opt-in=kotlinx.coroutines.InternalCoroutinesApi",
                        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
                    )
                )
            }

            this.dependsOn("ktfmtFormat")
        }

        withType<Test>() {
            useJUnitPlatform()
            testLogging { events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED) }
        }
    }

    plugins.withType<BasePlugin> {
        plugins.apply(libs.plugins.ktfmt.get().pluginId)
        ktfmt {
            kotlinLangStyle()
            removeUnusedImports = true
        }

        configure<BaseExtension> {
            compileSdkVersion(AndroidConfig.compileSdkVersion)
            defaultConfig {
                minSdk = AndroidConfig.minSdkVersion
                targetSdk = AndroidConfig.targetSdkVersion
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
                isCoreLibraryDesugaringEnabled = true
            }

            dependencies { add("coreLibraryDesugaring", libs.desugaring) }

            packagingOptions {
                resources.excludes.add("META-INF/LICENSE-LGPL-2.1.txt")
                resources.excludes.add("META-INF/LICENSE-LGPL-3.txt")
                resources.excludes.add("META-INF/LICENSE-W3C-TEST")
                resources.excludes.add("META-INF/DEPENDENCIES")
            }
        }
    }
}

tasks.register("clean", Delete::class) { delete(rootProject.layout.buildDirectory) }

tasks.register<Copy>("installGitHook") {
    from("pre-commit")
    into(layout.projectDirectory.dir(".git/hooks"))
}

afterEvaluate {
    // We install the hook at the first occasion
    tasks["clean"].dependsOn(tasks.getByName("installGitHook"))
}

tasks.register<KtfmtFormatTask>("ktfmtPrecommit")
