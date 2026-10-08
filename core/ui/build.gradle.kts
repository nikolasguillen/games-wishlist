plugins {
    id("questlog.kmp.compose")
}

compose.resources {
    // Other modules read this module's strings and drawables, so its Res class is public.
    publicResClass = true
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:designsystem"))
            implementation(libs.kotlinx.datetime)

            api(libs.jetbrains.material.icons.core)
            api(libs.jetbrains.material.icons.extended)
            implementation(libs.coil3.compose)
            api(libs.haze)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
        }
    }
}
