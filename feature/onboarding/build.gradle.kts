plugins {
    id("questlog.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.navigationevent.compose)
        }
    }
}
