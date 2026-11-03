import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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

// The macOS notifications library (native/macos). It is built by swiftc into the resources root of the Compose
// application, in the folder named after the OS and architecture, so run, runDistributable and packageDmg find it
// through the `compose.application.resources.dir` property. Never committed. Other systems build nothing.
val isMacOs = System.getProperty("os.name").startsWith("Mac")
val macNotificationsDirectory = layout.buildDirectory.dir(
    if (System.getProperty("os.arch") == "aarch64") "native-resources/macos-arm64" else "native-resources/macos-x64"
)
val buildMacNotifications by tasks.registering(Exec::class) {
    enabled = isMacOs
    val source = layout.projectDirectory.file("native/macos/QuestLogNotifications.swift")
    val library = macNotificationsDirectory.map { it.file("libQuestLogNotifications.dylib") }
    inputs.file(source)
    outputs.file(library)
    val output = library.get().asFile
    val input = source.asFile
    commandLine(
        "sh", "-c",
        "mkdir -p '${output.parent}' && swiftc -emit-library -o '$output' '$input' " +
            "-framework UserNotifications -framework Foundation"
    )
}
tasks.matching { it.name in setOf("prepareAppResources", "run", "runDistributable", "createDistributable") }
    .configureEach { dependsOn(buildMacNotifications) }

// What the app reports as its version (AppVersionProvider) and the installers carry.
val appVersion = "1.0.0"

compose.desktop {
    application {
        mainClass = "com.nikolasguillen.questlog.desktop.MainKt"
        jvmArgs += "-Dquestlog.version=$appVersion"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "QuestLog"
            packageVersion = appVersion
            appResourcesRootDir.set(layout.buildDirectory.dir("native-resources"))
            vendor = "Nikolas Guillen"
            // The JLink runtime keeps only these modules; `suggestRuntimeModules` lists what the app needs.
            modules("java.desktop", "java.instrument", "java.management", "java.naming", "java.sql", "jdk.httpserver", "jdk.unsupported")

            // Icons: `icons/` is exported from the launcher artwork, `androidApp/src/main/ic_launcher-playstore.png`.
            windows {
                menu = true
                shortcut = true
                perUserInstall = true
                // Fixed once so that a new installer upgrades the installed copy instead of installing beside it.
                upgradeUuid = "09DDB8C2-443B-4AB2-8BF9-381999F4290C"
                iconFile.set(project.file("icons/QuestLog.ico"))
            }
            macOS {
                bundleID = "com.nikolasguillen.questlog"
                iconFile.set(project.file("icons/QuestLog.icns"))
                infoPlist {
                    extraKeysRawXml = "<key>LSMinimumSystemVersion</key><string>13.0</string>"
                }
            }
        }
    }
}
