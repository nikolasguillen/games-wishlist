import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The desktop shell: the window, the single-instance guard and the notification click handling around `:shared`. It is
// a plain JVM module, hand-written like `:core:ai`, because the Compose application plugin packages a JVM application
// and a Kotlin Multiplatform module cannot be one. It reaches the app only through `:shared` (and `:core:common` for
// the platform helpers), never through `:core:data`, `:core:network`, `:core:database` or `:core:cloud`.
plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)
    // The installers bundle a JDK 21 runtime, but every module's desktop code targets 17 (specs/020-desktop-app, R1).
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:common"))

    implementation(compose.desktop.currentOs)
    // startQuestLogDesktop returns the Koin instance, and the compiler needs its class to name it.
    implementation(libs.koin.core)
    // The back keys feed the same navigation-event dispatcher that :shared's back handlers listen to.
    implementation(libs.jetbrains.navigationevent.compose)
    implementation(libs.jetbrains.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.jna)
    implementation(libs.jna.platform)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.desktop {
    application {
        mainClass = "com.nikolasguillen.questlog.desktop.MainKt"
        // What the app reports as its version (AppVersionProvider); the packaged copy gets the same value in T100.
        jvmArgs += "-Dquestlog.version=1.0.0"
    }
}
