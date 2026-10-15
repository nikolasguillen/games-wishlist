package com.nikolasguillen.questlog.core.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BackupChunkerTest {

    @Test
    fun `every part is at most the limit`() {
        val bytes = Random(1).nextBytes(500_000)

        val parts = bytes.toParts(maxChars = 1_000)

        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 1_000 })
    }

    @Test
    fun `the default limit is 700,000 characters`() {
        // 600,000 bytes are 800,000 base64 characters: two parts, the first one full.
        val parts = ByteArray(600_000).toParts()

        assertEquals(2, parts.size)
        assertEquals(PART_MAX_CHARS, parts.first().length)
        assertTrue(parts.all { it.length <= PART_MAX_CHARS })
    }

    @Test
    fun `parts rejoin to the identical bytes`() {
        val bytes = Random(2).nextBytes(123_457)

        val rejoined = bytes.toParts(maxChars = 997).joinParts(expectedLength = bytes.size.toLong())

        assertArrayEquals(bytes, rejoined)
    }

    @Test
    fun `an empty array yields no parts and rejoins to an empty array`() {
        val parts = ByteArray(0).toParts()

        assertTrue(parts.isEmpty())
        assertEquals(0, parts.joinParts(expectedLength = 0).size)
    }

    @Test
    fun `non-ASCII text survives the round trip`() {
        val text = "Zelda: Tears of the Kingdom - 王国之泪 - Ōkami 🎮"
        val bytes = text.encodeToByteArray()

        val rejoined = bytes.toParts(maxChars = 16).joinParts(expectedLength = bytes.size.toLong())

        assertEquals(text, rejoined.decodeToString())
    }

    @Test
    fun `a result whose length differs from the expected one is unreadable`() {
        val bytes = Random(3).nextBytes(1_000)
        val parts = bytes.toParts(maxChars = 100)

        assertThrows(BackupUnreadableException::class.java) {
            parts.joinParts(expectedLength = bytes.size + 1L)
        }
    }

    @Test
    fun `parts that are not base64 are unreadable`() {
        assertThrows(BackupUnreadableException::class.java) {
            listOf("not base64 !!").joinParts(expectedLength = 5)
        }
    }
}
