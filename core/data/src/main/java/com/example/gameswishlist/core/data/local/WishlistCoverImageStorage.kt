package com.example.gameswishlist.core.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Size
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Persists a gallery-picked image into app-private storage so it survives after the
 * picker's own URI grant expires (Photo Picker URIs aren't eligible for persistable
 * permissions), returning a stable file path to save alongside the list.
 *
 * The image is downscaled to at most [MAX_DIMENSION_PX] on its longest side, which is
 * enough for the largest place a cover is shown, and re-encoded so the file extension
 * always matches its actual content.
 */
class WishlistCoverImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val coversDir: File get() = File(context.filesDir, COVERS_DIR_NAME)

    suspend fun persist(sourceUri: String): String? = withContext(Dispatchers.IO) {
        var destinationFile: File? = null
        try {
            val bitmap = decodeDownscaled(sourceUri.toUri())
            // JPEG would flatten transparency to black, so keep alpha-bearing images lossless.
            val format = if (bitmap.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val file = File(coversDir.apply { mkdirs() }, "${UUID.randomUUID()}.${format.fileExtension}")
            destinationFile = file
            val compressed = file.outputStream().use { output ->
                bitmap.compress(format, COMPRESSION_QUALITY, output)
            }
            if (compressed) {
                file.absolutePath
            } else {
                file.delete()
                null
            }
        } catch (_: IOException) {
            destinationFile?.delete()
            null
        } catch (_: SecurityException) {
            destinationFile?.delete()
            null
        }
    }

    /**
     * Removes a cover previously returned by [persist]. Paths outside the covers directory are
     * ignored, so a stale or hand-edited value can never delete an unrelated file.
     */
    suspend fun delete(path: String): Unit = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.parentFile == coversDir) {
            file.delete()
        }
    }

    /**
     * Decodes [source] straight into the target size, so a full-resolution bitmap is never
     * held in memory. [ImageDecoder] also applies the EXIF orientation for us.
     */
    private fun decodeDownscaled(source: Uri): Bitmap {
        val decoderSource = ImageDecoder.createSource(context.contentResolver, source)
        return ImageDecoder.decodeBitmap(decoderSource) { decoder, info, _ ->
            // Hardware bitmaps can't be read back for compression.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val target = info.size.scaledToFit(MAX_DIMENSION_PX)
            decoder.setTargetSize(target.width, target.height)
        }
    }

    private fun Size.scaledToFit(maxDimension: Int): Size {
        val longestSide = maxOf(width, height)
        if (longestSide <= maxDimension) return this
        val scale = maxDimension.toFloat() / longestSide
        return Size(
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1)
        )
    }

    private val Bitmap.CompressFormat.fileExtension: String
        get() = if (this == Bitmap.CompressFormat.PNG) "png" else "jpg"

    private companion object {
        const val COVERS_DIR_NAME = "wishlist_covers"

        /**
         * Longest-side cap in pixels. Covers the full-bleed header of the wishlist detail
         * screen on a high-density phone without storing camera-resolution originals.
         */
        const val MAX_DIMENSION_PX = 1440

        const val COMPRESSION_QUALITY = 85
    }
}
