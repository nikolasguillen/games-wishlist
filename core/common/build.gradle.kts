plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.nikolasguillen.questlog.core.common"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // Part of DateUtils' public surface: parseIsoDate and timestampToLocalDate return its LocalDate.
    api(libs.kotlinx.datetime)
    implementation(libs.hilt.android)
    implementation(libs.koin.core)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
