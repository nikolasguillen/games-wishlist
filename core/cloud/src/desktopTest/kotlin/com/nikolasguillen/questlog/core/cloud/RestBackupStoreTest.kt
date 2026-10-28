package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupStreamDocument
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestBackupStoreTest {

    private class Recorded(val request: HttpRequestData, val body: String)

    private val recorded = mutableListOf<Recorded>()
    private var answers = ArrayDeque<Pair<HttpStatusCode, String>>()
    private var token = "id-token"

    private val store = RestBackupStore(
        httpClient = HttpClient(
            MockEngine { request ->
                recorded += Recorded(request, String(request.body.toByteArray()))
                val (status, body) = answers.removeFirstOrNull() ?: (HttpStatusCode.OK to "{}")
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        ),
        projectId = "the-project",
        tokens = { token }
    )

    private val base = "/v1/projects/the-project/databases/(default)/documents"

    private val manifest = BackupManifestDocument(
        "snap", 1, 1_700_000_000_000, "1.0.0", 1, 2, listOf(BackupStreamDocument("data", 5, listOf("snap-data-0")))
    )

    private fun answer(status: HttpStatusCode, body: String = "{}") = answers.addLast(status to body)

    @Test
    fun `reading the manifest asks for the user document with the token`() = runBlocking {
        answer(HttpStatusCode.OK, Json.encodeToString(FirestoreRestCodec.encode(manifest)))

        val read = store.readManifest("uid-1")

        assertEquals(manifest, read)
        val call = recorded.single().request
        assertEquals(HttpMethod.Get, call.method)
        assertEquals("$base/users/uid-1", call.url.encodedPath.replace("%28", "(").replace("%29", ")"))
        assertEquals("Bearer id-token", call.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `a missing manifest is null`() = runBlocking {
        answer(HttpStatusCode.NotFound, """{"error": {"code": 404, "status": "NOT_FOUND"}}""")

        assertNull(store.readManifest("uid-1"))
    }

    @Test
    fun `a missing part is null`() = runBlocking {
        answer(HttpStatusCode.NotFound)

        assertNull(store.readPart("uid-1", "snap-data-0"))
        assertTrue(recorded.single().request.url.encodedPath.endsWith("/users/uid-1/parts/snap-data-0"))
    }

    @Test
    fun `writing the manifest replaces the whole document`() = runBlocking {
        store.writeManifest("uid-1", manifest)

        val call = recorded.single()
        assertEquals(HttpMethod.Patch, call.request.method)
        assertTrue(call.request.url.encodedPath.endsWith("/users/uid-1"))
        // No updateMask: the patch replaces the document, as `set` does on the phones.
        assertFalse(call.request.url.parameters.contains("updateMask.fieldPaths"))
        val fields = Json.parseToJsonElement(call.body).jsonObject.getValue("fields").jsonObject
        assertEquals("snap", fields.getValue("snapshotId").jsonObject.getValue("stringValue").jsonPrimitive.content)
        assertEquals("Bearer id-token", call.request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `writing a part goes to its own document`() = runBlocking {
        store.writePart("uid-1", "snap-data-0", BackupPartDocument("snap", "data", 0, "QUJD"))

        val call = recorded.single()
        assertEquals(HttpMethod.Patch, call.request.method)
        assertTrue(call.request.url.encodedPath.endsWith("/users/uid-1/parts/snap-data-0"))
        assertTrue(call.body.contains("QUJD"))
    }

    @Test
    fun `listing parts follows the pages and reads only ids`() = runBlocking {
        answer(
            HttpStatusCode.OK,
            """{"documents": [{"name": "projects/p/databases/(default)/documents/users/uid-1/parts/a"},
                {"name": "projects/p/databases/(default)/documents/users/uid-1/parts/b"}], "nextPageToken": "next"}"""
        )
        answer(
            HttpStatusCode.OK,
            """{"documents": [{"name": "projects/p/databases/(default)/documents/users/uid-1/parts/c"}]}"""
        )

        val ids = store.listPartIds("uid-1")

        assertEquals(listOf("a", "b", "c"), ids)
        assertEquals(2, recorded.size)
        val first = recorded[0].request.url.parameters
        assertEquals("300", first["pageSize"])
        assertEquals("snapshotId", first["mask.fieldPaths"])
        assertNull(first["pageToken"])
        assertEquals("next", recorded[1].request.url.parameters["pageToken"])
    }

    @Test
    fun `listing no parts is an empty list`() = runBlocking {
        assertEquals(emptyList<String>(), store.listPartIds("uid-1"))
    }

    @Test
    fun `an unauthorised answer is unauthenticated`() = runBlocking {
        answer(HttpStatusCode.Unauthorized)

        try {
            store.readPart("uid-1", "p")
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unauthenticated) {
            // expected
        }
    }

    @Test
    fun `a server error is unknown`() = runBlocking {
        answer(HttpStatusCode.InternalServerError)

        try {
            store.deleteManifest("uid-1")
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unknown) {
            // expected
        }
    }

    @Test
    fun `deleting the manifest sends a delete`() = runBlocking {
        store.deleteManifest("uid-1")

        assertEquals(HttpMethod.Delete, recorded.single().request.method)
        assertTrue(recorded.single().request.url.encodedPath.endsWith("/users/uid-1"))
    }

    @Test
    fun `deleting 401 parts takes two commits of at most 400`() = runBlocking {
        val ids = (1..401).map { "part-$it" }

        store.deleteParts("uid-1", ids)

        assertEquals(2, recorded.size)
        recorded.forEach {
            assertEquals(HttpMethod.Post, it.request.method)
            assertTrue(it.request.url.encodedPath.endsWith("documents:commit"))
        }
        val writes = recorded.map { Json.parseToJsonElement(it.body).jsonObject.getValue("writes").jsonArray }
        assertEquals(listOf(400, 1), writes.map { it.size })
        assertEquals(
            "projects/the-project/databases/(default)/documents/users/uid-1/parts/part-1",
            writes[0][0].jsonObject.getValue("delete").jsonPrimitive.content
        )
        assertEquals(
            ids,
            writes.flatMap { w -> w.map { it.jsonObject.getValue("delete").jsonPrimitive.content.substringAfterLast('/') } }
        )
    }

    @Test
    fun `deleting no parts sends nothing`() = runBlocking {
        store.deleteParts("uid-1", emptyList())

        assertTrue(recorded.isEmpty())
    }

    @Test
    fun `without a token nothing is sent`() = runBlocking {
        val unauthenticated = RestBackupStore(
            HttpClient(MockEngine { respond("{}") }), "p", tokens = { throw CloudException.Unauthenticated("no") }
        )

        try {
            unauthenticated.readManifest("uid-1")
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unauthenticated) {
            assertTrue(recorded.isEmpty())
        }
    }
}
