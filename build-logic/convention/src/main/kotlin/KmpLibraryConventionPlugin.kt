import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `questlog.kmp.library`: a multiplatform library module with an Android target and the two iOS targets.
 *
 * It owns what every module used to repeat by hand — `compileSdk`, `minSdk`, Java 11 and the namespace — and
 * derives the namespace from the project path, so `:feature:game-detail` becomes
 * `com.nikolasguillen.questlog.feature.gamedetail`, exactly what it was before the conversion.
 *
 * Android host tests live in `androidHostTest` and run with `testAndroidHostTest`.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")

            extensions.configure<KotlinMultiplatformExtension> {
                iosArm64()
                iosSimulatorArm64()

                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    namespace = questLogNamespace()
                    compileSdk = COMPILE_SDK
                    minSdk = MIN_SDK
                    withHostTest {}
                    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    private companion object {
        const val COMPILE_SDK = 37
        const val MIN_SDK = 29
    }
}
