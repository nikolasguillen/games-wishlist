import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `questlog.android.application`: the Android entry point. It owns the SDK levels and Java 11 so `:androidApp`'s build
 * file keeps only what is its own: the application id, versions, signing and shrinking.
 *
 * `:androidApp` cannot be a multiplatform module (AGP 9 forbids it next to `com.android.application`), so everything
 * shared lives in `:shared`.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                compileSdk = COMPILE_SDK
                defaultConfig {
                    minSdk = MIN_SDK
                    targetSdk = TARGET_SDK
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
                buildFeatures {
                    compose = true
                }
            }
        }
    }

    private companion object {
        const val COMPILE_SDK = 37
        const val MIN_SDK = 29
        const val TARGET_SDK = 37
    }
}
