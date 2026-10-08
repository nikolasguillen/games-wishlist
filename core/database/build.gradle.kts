plugins {
    id("questlog.kmp.library")
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.androidx.room)
}

// Room writes a JSON description of every schema version here, and the directory is checked in: it is
// what a future Migration gets written and tested against, and what tells a reviewer that a column
// changed. Without it Room cannot verify a migration at all.
room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
