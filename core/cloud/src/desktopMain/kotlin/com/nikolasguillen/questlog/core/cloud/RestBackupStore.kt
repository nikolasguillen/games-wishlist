package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** A commit takes 500 writes; stay well under it, as the mobile store does for its batches. */
private const val DELETE_BATCH_SIZE = 400

private const val LIST_PAGE_SIZE = 300

/**
 * [CloudBackupStore] for desktop over the Firestore REST API (contracts/firestore-rest.md), against the same documents
 * the phones use: `users/{uid}` for the manifest and `users/{uid}/parts/{partId}` for the data.
 *
 * Writes send the whole document and no `updateMask`, which replaces it, as `set` does on the phones. Every call
 * carries the signed-in user's ID token, is bounded by [CLOUD_CALL_TIMEOUT] and fails only as a `CloudException`.
 */
internal class RestBackupStore(
    private val httpClient: HttpClient,
    private val projectId: String,
    private val tokens: IdTokenSupplier
) : CloudBackupStore {

    private val documents = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    override suspend fun readManifest(uid: String): BackupManifestDocument? =
        read("$documents/users/$uid")?.let(FirestoreRestCodec::decodeManifest)

    override suspend fun writeManifest(uid: String, manifest: BackupManifestDocument) {
        send(HttpMethod.Patch, "$documents/users/$uid", body = FirestoreRestCodec.encode(manifest))
    }

    override suspend fun readPart(uid: String, partId: String): BackupPartDocument? =
        read("$documents/users/$uid/parts/$partId")?.let(FirestoreRestCodec::decodePart)

    override suspend fun writePart(uid: String, partId: String, part: BackupPartDocument) {
        send(HttpMethod.Patch, "$documents/users/$uid/parts/$partId", body = FirestoreRestCodec.encode(part))
    }

    override suspend fun listPartIds(uid: String): List<String> {
        val ids = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val page = json(
                send(
                    HttpMethod.Get,
                    "$documents/users/$uid/parts",
                    query = buildMap {
                        put("pageSize", LIST_PAGE_SIZE.toString())
                        // Only the ids are wanted, so the parts' data (up to 700 KB each) is not downloaded.
                        put("mask.fieldPaths", "snapshotId")
                        pageToken?.let { put("pageToken", it) }
                    }
                ).bodyAsText()
            )
            page["documents"]?.jsonArray.orEmpty().mapTo(ids) {
                it.jsonObject.getValue("name").jsonPrimitive.content.substringAfterLast('/')
            }
            pageToken = page["nextPageToken"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        return ids
    }

    override suspend fun deleteParts(uid: String, partIds: Collection<String>) {
        partIds.chunked(DELETE_BATCH_SIZE).forEach { chunk ->
            val commit = buildJsonObject {
                putJsonArray("writes") {
                    chunk.forEach { id ->
                        add(
                            buildJsonObject {
                                put("delete", "projects/$projectId/databases/(default)/documents/users/$uid/parts/$id")
                            }
                        )
                    }
                }
            }
            send(HttpMethod.Post, "$documents:commit", body = commit)
        }
    }

    override suspend fun deleteManifest(uid: String) {
        send(HttpMethod.Delete, "$documents/users/$uid")
    }

    /** The document at [address], or `null` when there is none. */
    private suspend fun read(address: String): JsonObject? {
        val response = send(HttpMethod.Get, address, allowNotFound = true)
        return if (response.status == HttpStatusCode.NotFound) null else json(response.bodyAsText())
    }

    /** One request with the user's token; a non-2xx answer becomes the matching exception. */
    private suspend fun send(
        method: HttpMethod,
        address: String,
        body: JsonObject? = null,
        query: Map<String, String> = emptyMap(),
        allowNotFound: Boolean = false
    ): HttpResponse = RestErrorMapper.call {
        val token = tokens.idToken()
        val response = httpClient.request(address) {
            this.method = method
            header(HttpHeaders.Authorization, "Bearer $token")
            query.forEach { (name, value) -> parameter(name, value) }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }
        val missing = allowNotFound && response.status == HttpStatusCode.NotFound
        if (!missing && !response.status.isSuccess()) throw RestErrorMapper.fromFirestoreStatus(response.status.value)
        response
    }

    private fun json(text: String): JsonObject = try {
        Json.parseToJsonElement(text).jsonObject
    } catch (e: Exception) {
        throw CloudException.Unknown("Firestore answered with something that is not JSON", e)
    }
}
