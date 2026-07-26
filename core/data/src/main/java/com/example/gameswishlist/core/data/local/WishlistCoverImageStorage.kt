package com.example.gameswishlist.core.data.local

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Persists a gallery-picked image into app-private storage so it survives after the
 * picker's own URI grant expires (Photo Picker URIs aren't eligible for persistable
 * permissions), returning a stable file path to save alongside the list.
 */
class WishlistCoverImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun persist(sourceUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val coversDir = File(context.filesDir, COVERS_DIR_NAME).apply { mkdirs() }
            val destinationFile = File(coversDir, "${UUID.randomUUID()}.jpg")
            val copied = context.contentResolver.openInputStream(sourceUri.toUri())?.use { input ->
                destinationFile.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) destinationFile.absolutePath else null
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private companion object {
        const val COVERS_DIR_NAME = "wishlist_covers"
    }
}
