import java.util.Properties

plugins {
    id("questlog.kmp.library")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.buildconfig)
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

// Generates `internal object IgdbCredentials` into commonMain's generated sources, under build/. The values come
// from local.properties and the generated file is never committed.
buildConfig {
    packageName("com.nikolasguillen.questlog.core.network")
    className("IgdbCredentials")
    useKotlinOutput { internalVisibility = true }
    buildConfigField("IGDB_CLIENT_ID", localProps.getProperty("IGDB_CLIENT_ID") ?: "")
    buildConfigField("IGDB_CLIENT_SECRET", localProps.getProperty("IGDB_CLIENT_SECRET") ?: "")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))

            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.io.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        iosTest.dependencies {
            implementation(kotlin("test"))
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
