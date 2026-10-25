plugins {
    id("questlog.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            // Part of DateUtils' public surface: parseIsoDate and timestampToLocalDate return its LocalDate.
            api(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
        // JNA reaches the Windows and macOS notification APIs (specs/020-desktop-app, research R7).
        getByName("desktopMain").dependencies {
            implementation(libs.jna)
            implementation(libs.jna.platform)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
        }
    }
}
