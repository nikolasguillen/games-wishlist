// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
    // The multiplatform plugins are applied by the convention plugins in build-logic; declaring them here puts
    // them on the build's plugin classpath once, at one version.
    alias(libs.plugins.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.buildconfig) apply false
}

// A multiplatform module has no `test` task: its JVM tests are `testAndroidHostTest`. This root task keeps
// `./gradlew test` meaning "every module's unit tests". It is not `allTests`, which would also run the iOS simulator
// tests.
tasks.register("test") {
    group = "verification"
    description = "Runs the Android host tests of every multiplatform module."
    subprojects.forEach { module ->
        module.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            dependsOn(module.tasks.named("testAndroidHostTest"))
        }
    }
}
