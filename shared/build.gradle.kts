plugins {
    id("questlog.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Everything the app is made of except :core:ai, which only :core:data may reach.
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:domain"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:navigation"))
            implementation(project(":feature:radar"))
            implementation(project(":feature:search"))
            implementation(project(":feature:game-detail"))
            implementation(project(":feature:lists"))
            implementation(project(":feature:wishlist"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:onboarding"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
            implementation(libs.jetbrains.material.icons.extended)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.koin.test.junit4)
            // WorkerParameters is a type the graph test declares as supplied at runtime.
            implementation(libs.androidx.work.runtime.ktx)
        }
    }
}
