import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("questlog.kmp.library")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.buildconfig)
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

// Generates `internal object DesktopCloudCredentials` into desktopMain's generated sources, under build/, so the keys
// never reach the Android or iOS binaries. The values come from local.properties and the generated file is never
// committed. A missing key is an empty string: the build still works and signing in fails at run time (R6).
buildConfig {
    sourceSets.named("desktopMain") {
        packageName("com.nikolasguillen.questlog.core.cloud")
        className("DesktopCloudCredentials")
        useKotlinOutput { internalVisibility = true }
        buildConfigField("FIREBASE_WEB_API_KEY", localProps.getProperty("FIREBASE_WEB_API_KEY") ?: "")
        buildConfigField("FIREBASE_PROJECT_ID", localProps.getProperty("FIREBASE_PROJECT_ID") ?: "")
        buildConfigField("GOOGLE_DESKTOP_CLIENT_ID", localProps.getProperty("GOOGLE_DESKTOP_CLIENT_ID") ?: "")
        buildConfigField("GOOGLE_DESKTOP_CLIENT_SECRET", localProps.getProperty("GOOGLE_DESKTOP_CLIENT_SECRET") ?: "")
    }
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
        getByName("desktopMain").dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        getByName("desktopTest").dependencies {
            implementation(libs.ktor.client.mock)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockk)
        }
    }
}
