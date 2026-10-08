plugins {
    `kotlin-dsl`
}

group = "com.nikolasguillen.questlog.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.composeGradlePlugin)
    compileOnly(libs.jetbrains.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "questlog.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
    }
}
