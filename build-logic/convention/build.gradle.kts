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
        register("kmpCompose") {
            id = "questlog.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "questlog.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("kmpFeature") {
            id = "questlog.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
    }
}
