import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("questlog.kmp.library")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    // `mobileMain` is Android and iOS, the two platforms that reach Firebase through GitLive. Desktop has no Firebase
    // SDK that can sign in with a Google credential, so it uses the REST client in `desktopMain` instead (R3).
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("mobile") {
                // `withAndroidTarget()` only matches the legacy Android plugin's target, not this module's.
                withCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
                group("ios") {
                    withIos()
                }
            }
        }
    }

    // GitLive's `set` and `data` are inline functions built for JVM 17, and Kotlin cannot inline them into Java 11
    // bytecode. Only this module calls them, and the app already runs GitLive's own 17 bytecode, so the exception stays
    // here instead of moving the convention plugin's target.
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }
        val mobileMain by getting {
            dependencies {
                implementation(libs.gitlive.firebase.auth)
                implementation(libs.gitlive.firebase.firestore)
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockk)
        }
    }
}
