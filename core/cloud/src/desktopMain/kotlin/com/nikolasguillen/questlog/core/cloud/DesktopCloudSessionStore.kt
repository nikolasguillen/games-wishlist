package com.nikolasguillen.questlog.core.cloud

import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions

private const val SESSION_FILE_NAME = "cloud-session.json"

/**
 * Keeps the signed-in [DesktopCloudSession] in `cloud-session.json` under [directory], the way Firebase's own SDKs keep
 * theirs: in a per-user file, so anything that runs as the user can read it (research R5).
 *
 * The directory is a parameter because `:core:cloud` does not know where the app keeps its data; the app passes its own.
 */
internal class DesktopCloudSessionStore(private val directory: Path) {

    private val file: Path get() = directory.resolve(SESSION_FILE_NAME)

    /** The stored session, or `null` when there is none or the file is unreadable: that is just "signed out". */
    fun load(): DesktopCloudSession? = try {
        if (Files.exists(file)) Json.decodeFromString<DesktopCloudSession>(Files.readString(file)) else null
    } catch (_: Exception) {
        // Missing, truncated, hand-edited or from another version: the user signs in again. Nothing to report.
        null
    }

    /** Replaces the stored session. A crash halfway leaves the previous file, never a half-written one. */
    fun save(session: DesktopCloudSession) {
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, "cloud-session", ".tmp")
        try {
            // Owner-only before the secret goes in. Windows has no POSIX permissions: its per-user profile covers it.
            runCatching { Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------")) }
            Files.writeString(temporary, Json.encodeToString(session), StandardCharsets.UTF_8)
            move(temporary)
        } catch (e: IOException) {
            runCatching { Files.deleteIfExists(temporary) }
            throw e
        }
    }

    /** Forgets the session. */
    fun clear() {
        Files.deleteIfExists(file)
    }

    private fun move(temporary: Path) {
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
