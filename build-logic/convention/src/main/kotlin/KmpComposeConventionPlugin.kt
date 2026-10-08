import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `questlog.kmp.compose`: [KmpLibraryConventionPlugin] plus Compose Multiplatform.
 *
 * Compose resources are generated into `<namespace>.resources`, so a module's `Res` is always in its own
 * package. Android resources are enabled for the Android-only files a module may still carry.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("questlog.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    androidResources { enable = true }
                }
                // The Android target runs the Jetpack Material 3 the catalog pins (the app pins the same one), which
                // is newer than the alpha the multiplatform artifact is built on. Compiling against it keeps the
                // compile classpath equal to the runtime one.
                sourceSets.getByName("androidMain").dependencies {
                    implementation(lib("androidx-compose-material3-versioned"))
                }
                sourceSets.getByName("commonMain").dependencies {
                    implementation(lib("jetbrains-compose-runtime"))
                    implementation(lib("jetbrains-compose-foundation"))
                    implementation(lib("jetbrains-compose-ui"))
                    implementation(lib("jetbrains-compose-material3"))
                    implementation(lib("jetbrains-compose-components-resources"))
                    implementation(lib("jetbrains-compose-ui-tooling-preview"))
                }
            }

            extensions.configure<ComposeExtension> {
                (this as ExtensionAware).extensions.configure<ResourcesExtension>("resources") {
                    packageOfResClass = "${questLogNamespace()}.resources"
                }
            }

            // @Preview rendering in Android Studio needs the tooling at runtime; KMP Android has no debug variant.
            dependencies {
                add("androidRuntimeClasspath", lib("jetbrains-compose-ui-tooling"))
            }
        }
    }
}
