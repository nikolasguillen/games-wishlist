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
        androidHostTest.dependencies {
            implementation(libs.junit)
        }
    }
}
