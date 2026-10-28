package com.nikolasguillen.questlog.core.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

class DesktopCloudSessionStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val session = DesktopCloudSession(
        uid = "uid-1",
        email = "player@example.com",
        providers = listOf("google.com"),
        refreshToken = "secret-refresh-token"
    )

    private fun store() = DesktopCloudSessionStore(folder.root.toPath())

    @Test
    fun `a saved session is loaded back`() {
        store().save(session)

        assertEquals(session, store().load())
    }

    @Test
    fun `saving again replaces the session`() {
        store().save(session)
        store().save(session.copy(refreshToken = "rotated"))

        assertEquals("rotated", store().load()!!.refreshToken)
    }

    @Test
    fun `no file means signed out`() {
        assertNull(store().load())
    }

    @Test
    fun `a corrupt file means signed out and not a crash`() {
        Files.writeString(folder.root.toPath().resolve("cloud-session.json"), "{ not json")

        assertNull(store().load())
    }

    @Test
    fun `a file of another shape means signed out`() {
        Files.writeString(folder.root.toPath().resolve("cloud-session.json"), """{"uid": 5}""")

        assertNull(store().load())
    }

    @Test
    fun `clearing forgets the session and tolerates there being none`() {
        store().clear()
        store().save(session)
        store().clear()

        assertNull(store().load())
    }

    @Test
    fun `no temporary file is left behind`() {
        store().save(session)

        assertEquals(listOf("cloud-session.json"), Files.list(folder.root.toPath()).use { it.map { p -> p.fileName.toString() }.toList() })
    }

    @Test
    fun `the file is private to its owner where the system has permissions`() {
        store().save(session)
        val file = folder.root.toPath().resolve("cloud-session.json")
        if (!file.fileSystem.supportedFileAttributeViews().contains("posix")) return

        val permissions = Files.getPosixFilePermissions(file)

        assertEquals(setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), permissions)
    }

    @Test
    fun `the secret does not appear in the printed session`() {
        assertFalse(session.toString().contains("secret-refresh-token"))
        assertTrue(session.toString().contains("uid-1"))
    }
}
