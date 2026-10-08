plugins {
    id("questlog.kmp.library")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.kotlinx.serialization.core)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlin.reflect)
        }
    }
}
