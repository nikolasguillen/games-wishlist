plugins {
    id("questlog.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.coil3.compose)
        }
    }
}
