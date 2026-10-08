plugins {
    id("questlog.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.ui.backhandler)
        }
    }
}
