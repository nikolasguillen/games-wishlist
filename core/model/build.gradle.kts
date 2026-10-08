plugins {
    id("questlog.kmp.library")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.core)
        }
    }
}
