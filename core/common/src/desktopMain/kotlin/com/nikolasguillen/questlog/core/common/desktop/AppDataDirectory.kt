package com.nikolasguillen.questlog.core.common.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The per-user directory that holds everything the desktop app writes: the database, the settings store, the cover
 * images, the cloud session and the single-instance lock.
 *
 * It is `%LOCALAPPDATA%\QuestLog` on Windows (local, not roaming: a database does not belong in a roaming profile)
 * and `~/Library/Application Support/QuestLog` on macOS. The directory is created on first use.
 */
fun appDataDirectory(): Path = directory.also { Files.createDirectories(it) }

private val directory: Path by lazy {
    val home = Paths.get(System.getProperty("user.home"))
    if (isMacOs()) {
        home.resolve("Library").resolve("Application Support").resolve(APP_DIRECTORY_NAME)
    } else {
        val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
        (localAppData?.let(Paths::get) ?: home.resolve("AppData").resolve("Local")).resolve(APP_DIRECTORY_NAME)
    }
}

private const val APP_DIRECTORY_NAME = "QuestLog"
