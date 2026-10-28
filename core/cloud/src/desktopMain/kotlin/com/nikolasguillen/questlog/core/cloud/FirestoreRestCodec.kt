package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupStreamDocument
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Turns the backup documents into Firestore's REST value format and back (contracts/firestore-rest.md). The field
 * names are the ones GitLive's serializer writes on the phones, so a document written here reads there and the other way
 * around. Pure functions: no network and no state.
 */
internal object FirestoreRestCodec {

    fun encode(manifest: BackupManifestDocument): JsonObject = document {
        put("snapshotId", string(manifest.snapshotId))
        put("formatVersion", integer(manifest.formatVersion.toLong()))
        put("createdAtEpochMillis", integer(manifest.createdAtEpochMillis))
        put("appVersion", string(manifest.appVersion))
        put("listCount", integer(manifest.listCount.toLong()))
        put("gameCount", integer(manifest.gameCount.toLong()))
        put("streams", array(manifest.streams.map { map(encodeStream(it)) }))
    }

    fun encode(part: BackupPartDocument): JsonObject = document {
        put("snapshotId", string(part.snapshotId))
        put("stream", string(part.stream))
        put("index", integer(part.index.toLong()))
        put("data", string(part.data))
    }

    fun decodeManifest(document: JsonObject): BackupManifestDocument = malformedAsUnknown {
        val fields = document.fields()
        BackupManifestDocument(
            snapshotId = fields.string("snapshotId"),
            formatVersion = fields.long("formatVersion").toInt(),
            createdAtEpochMillis = fields.long("createdAtEpochMillis"),
            appVersion = fields.string("appVersion"),
            listCount = fields.long("listCount").toInt(),
            gameCount = fields.long("gameCount").toInt(),
            streams = fields.values("streams").map { decodeStream(it.mapFields()) }
        )
    }

    fun decodePart(document: JsonObject): BackupPartDocument = malformedAsUnknown {
        val fields = document.fields()
        BackupPartDocument(
            snapshotId = fields.string("snapshotId"),
            stream = fields.string("stream"),
            index = fields.long("index").toInt(),
            data = fields.string("data")
        )
    }

    private fun encodeStream(stream: BackupStreamDocument): JsonObject = buildJsonObject {
        put("name", string(stream.name))
        put("byteLength", integer(stream.byteLength))
        put("partIds", array(stream.partIds.map(::string)))
    }

    private fun decodeStream(fields: JsonObject) = BackupStreamDocument(
        name = fields.string("name"),
        byteLength = fields.long("byteLength"),
        partIds = fields.values("partIds").map { it.jsonObject.getValue("stringValue").jsonPrimitive.content }
    )

    private fun document(fields: JsonObjectBuilder.() -> Unit): JsonObject =
        buildJsonObject { putJsonObject("fields", fields) }

    private fun string(value: String): JsonObject = buildJsonObject { put("stringValue", value) }

    // The API takes 64-bit integers as decimal strings.
    private fun integer(value: Long): JsonObject = buildJsonObject { put("integerValue", value.toString()) }

    private fun map(fields: JsonObject): JsonObject = buildJsonObject {
        putJsonObject("mapValue") { put("fields", fields) }
    }

    private fun array(values: List<JsonObject>): JsonObject = buildJsonObject {
        putJsonObject("arrayValue") { putJsonArray("values") { values.forEach { add(it) } } }
    }

    private fun JsonObject.fields(): JsonObject = getValue("fields").jsonObject

    private fun JsonElement.mapFields(): JsonObject = jsonObject.getValue("mapValue").jsonObject.fields()

    private fun JsonObject.string(name: String): String =
        getValue(name).jsonObject.getValue("stringValue").jsonPrimitive.content

    private fun JsonObject.long(name: String): Long {
        val value = getValue(name).jsonObject
        // `doubleValue` is not what the phones write, but is accepted rather than failing a restore over it.
        value["integerValue"]?.jsonPrimitive?.content?.let { return it.toLong() }
        return value.getValue("doubleValue").jsonPrimitive.content.toDouble().toLong()
    }

    // An empty array comes back as `{"arrayValue": {}}`, with no `values` at all.
    private fun JsonObject.values(name: String): List<JsonElement> =
        getValue(name).jsonObject.getValue("arrayValue").jsonObject["values"]?.jsonArray.orEmpty()

    private inline fun <T> malformedAsUnknown(decode: () -> T): T = try {
        decode()
    } catch (e: CloudException) {
        throw e
    } catch (e: Exception) {
        // A document that is not shaped like a backup (missing field, wrong value type, text where a number belongs).
        throw CloudException.Unknown("The backup document is malformed", e)
    }
}
