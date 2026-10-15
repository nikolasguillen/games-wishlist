plugins {
    id("questlog.kmp.library")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
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
