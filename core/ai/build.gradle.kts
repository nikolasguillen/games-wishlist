plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.nikolasguillen.questlog.core.ai"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Travels with the module that owns the ML Kit dependency, so :androidApp's R8 run gets the rule
        // without :androidApp having to know what :core:ai wraps.
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.mlkit.genai.prompt)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
