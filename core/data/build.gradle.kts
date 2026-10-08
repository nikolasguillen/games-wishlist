plugins {
    id("questlog.kmp.library")
}

kotlin {
    android {
        // The release-reminder notification text lives in this module's own strings.xml.
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:domain"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))

            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.datastore.preferences.core)
            // :core:network's exceptions extend kotlinx.io.IOException, and the compiler needs it to name them.
            implementation(libs.kotlinx.io.core)
        }
        androidMain.dependencies {
            // ML Kit's on-device GenAI client has no multiplatform counterpart: Android only, reachable only from here.
            implementation(project(":core:ai"))

            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.koin.androidx.workmanager)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockk)
        }
    }
}
