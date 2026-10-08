plugins {
    id("questlog.kmp.compose")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            // WindowCompat, for the status and navigation bar icon colours.
            implementation(libs.androidx.core.ktx)
        }
    }
}
