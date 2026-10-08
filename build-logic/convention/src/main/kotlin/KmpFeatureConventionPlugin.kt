import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `questlog.kmp.feature`: [KmpComposeConventionPlugin] plus what every feature module needs and nothing more.
 *
 * The six `:core:*` modules are the only ones a feature may depend on; `:core:data`, `:core:network`,
 * `:core:database` and `:core:ai` are deliberately absent, so a feature cannot reach them by accident.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("questlog.kmp.compose")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":core:common"))
                    implementation(project(":core:model"))
                    implementation(project(":core:domain"))
                    implementation(project(":core:ui"))
                    implementation(project(":core:navigation"))
                    implementation(project(":core:designsystem"))

                    implementation(lib("jetbrains-lifecycle-viewmodel-compose"))
                    implementation(lib("jetbrains-lifecycle-runtime-compose"))
                    implementation(lib("kotlinx-coroutines-core"))
                }
                sourceSets.getByName("androidHostTest").dependencies {
                    implementation(lib("junit"))
                    implementation(lib("mockk"))
                    implementation(lib("kotlinx-coroutines-test"))
                }
            }
        }
    }
}
