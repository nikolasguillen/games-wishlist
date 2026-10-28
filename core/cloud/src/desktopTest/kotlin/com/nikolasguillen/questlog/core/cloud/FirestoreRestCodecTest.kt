package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupStreamDocument
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FirestoreRestCodecTest {

    private val manifest = BackupManifestDocument(
        snapshotId = "snap-1",
        formatVersion = 3,
        createdAtEpochMillis = 1_700_000_000_123,
        appVersion = "1.0.0",
        listCount = 2,
        gameCount = 41,
        streams = listOf(
            BackupStreamDocument(name = "data", byteLength = 9_876_543_210, partIds = listOf("snap-1-data-0", "snap-1-data-1")),
            BackupStreamDocument(name = "cover-7", byteLength = 12, partIds = listOf("snap-1-cover-7-0"))
        )
    )

    @Test
    fun `a manifest survives a round trip`() {
        assertEquals(manifest, FirestoreRestCodec.decodeManifest(FirestoreRestCodec.encode(manifest)))
    }

    @Test
    fun `integers are written as decimal strings`() {
        val fields = FirestoreRestCodec.encode(manifest).getValue("fields").jsonObject

        assertEquals("3", fields.getValue("formatVersion").jsonObject.getValue("integerValue").jsonPrimitive.content)
        assertEquals(
            "1700000000123",
            fields.getValue("createdAtEpochMillis").jsonObject.getValue("integerValue").jsonPrimitive.content
        )
    }

    @Test
    fun `a part survives a round trip with the largest data`() {
        val part = BackupPartDocument(snapshotId = "snap-1", stream = "data", index = 4, data = "A".repeat(700_000))

        assertEquals(part, FirestoreRestCodec.decodePart(FirestoreRestCodec.encode(part)))
    }

    @Test
    fun `a manifest written by the phones reads`() {
        // The shape Firestore returns for a document GitLive wrote.
        val fixture = parse(
            """
            {
              "name": "projects/p/databases/(default)/documents/users/u",
              "fields": {
                "snapshotId": {"stringValue": "abc"},
                "formatVersion": {"integerValue": "2"},
                "createdAtEpochMillis": {"integerValue": "1700000000000"},
                "appVersion": {"stringValue": "1.4.0"},
                "listCount": {"integerValue": "1"},
                "gameCount": {"integerValue": "0"},
                "streams": {"arrayValue": {"values": [
                  {"mapValue": {"fields": {
                    "name": {"stringValue": "data"},
                    "byteLength": {"integerValue": "100"},
                    "partIds": {"arrayValue": {"values": [{"stringValue": "abc-data-0"}]}}
                  }}}
                ]}}
              },
              "createTime": "2026-01-01T00:00:00Z",
              "updateTime": "2026-01-01T00:00:00Z"
            }
            """
        )

        val manifest = FirestoreRestCodec.decodeManifest(fixture)

        assertEquals("abc", manifest.snapshotId)
        assertEquals(1_700_000_000_000, manifest.createdAtEpochMillis)
        assertEquals(listOf(BackupStreamDocument("data", 100, listOf("abc-data-0"))), manifest.streams)
    }

    @Test
    fun `an empty array reads as an empty list`() {
        val fixture = parse(
            """
            {"fields": {
              "snapshotId": {"stringValue": "abc"},
              "formatVersion": {"integerValue": "2"},
              "createdAtEpochMillis": {"integerValue": "1"},
              "appVersion": {"stringValue": ""},
              "listCount": {"integerValue": "0"},
              "gameCount": {"integerValue": "0"},
              "streams": {"arrayValue": {}}
            }}
            """
        )

        assertEquals(emptyList<BackupStreamDocument>(), FirestoreRestCodec.decodeManifest(fixture).streams)
    }

    @Test
    fun `a double value is accepted where a number is expected`() {
        val fixture = parse(
            """
            {"fields": {
              "snapshotId": {"stringValue": "s"},
              "stream": {"stringValue": "data"},
              "index": {"doubleValue": 3.0},
              "data": {"stringValue": "x"}
            }}
            """
        )

        assertEquals(3, FirestoreRestCodec.decodePart(fixture).index)
    }

    @Test
    fun `a document that is not a backup is an unknown failure`() {
        assertThrows(CloudException.Unknown::class.java) {
            FirestoreRestCodec.decodePart(parse("""{"fields": {"snapshotId": {"integerValue": "1"}}}"""))
        }
        assertThrows(CloudException.Unknown::class.java) { FirestoreRestCodec.decodeManifest(parse("{}")) }
    }

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject
}
