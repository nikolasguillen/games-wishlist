package com.nikolasguillen.questlog.core.data.local

import com.nikolasguillen.questlog.core.common.desktop.appDataDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.math.max
import kotlin.math.roundToInt

private const val COVERS_DIR_NAME = "covers"

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

// Longest side in pixels, the same cap as on Android and iOS: enough for the full-bleed header of the wishlist detail
// screen without storing camera-resolution originals.
private const val MAX_DIMENSION_PX = 1440
private const val JPEG_QUALITY = 0.85f

/**
 * Desktop implementation of [WishlistCoverImageStorage]. The picker hands over the path of a file the user owns; this
 * decodes it, downscales it to [MAX_DIMENSION_PX] and writes the result into `<app data>/covers`, so a cover outlives
 * the original being moved or deleted.
 *
 * Re-encoded so the extension always matches the content: PNG when the image has transparency, JPEG otherwise. The
 * source file is never touched, because unlike the temporary file an iOS picker hands over it is the user's own.
 */
internal class WishlistCoverImageStorageImpl : WishlistCoverImageStorage {

    private val coversDir: Path get() = appDataDirectory().resolve(COVERS_DIR_NAME)

    override suspend fun persist(source: String): String? = withContext(Dispatchers.IO) {
        var destination: Path? = null
        try {
            val decoded = ImageIO.read(Paths.get(source).toFile()) ?: return@withContext null
            val hasAlpha = decoded.colorModel.hasAlpha()
            val scaled = decoded.scaledToFit(hasAlpha)
            val extension = if (hasAlpha) "png" else "jpg"
            val file = Files.createDirectories(coversDir).resolve("${UUID.randomUUID()}.$extension")
            destination = file
            val written = if (hasAlpha) {
                ImageIO.write(scaled, "png", file.toFile())
            } else {
                writeJpeg(scaled, file)
            }
            if (written) {
                file.toString()
            } else {
                file.deleteIfExists()
                null
            }
        } catch (_: IOException) {
            destination?.deleteIfExists()
            null
        } catch (_: SecurityException) {
            destination?.deleteIfExists()
            null
        }
    }

    override suspend fun delete(path: String): Unit = withContext(Dispatchers.IO) {
        // Only files this class wrote: a stale or hand-edited value can never delete an unrelated one.
        val file = insideCovers(path) ?: return@withContext
        try {
            file.deleteIfExists()
        } catch (_: IOException) {
            // Best effort, like the other platforms: a cover that stays on disk is only wasted space.
        }
    }

    override suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = insideCovers(path) ?: return@withContext null
        try {
            Files.readAllBytes(file)
        } catch (_: IOException) {
            null
        }
    }

    override suspend fun import(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        val extension = if (bytes.startsWithPngSignature()) "png" else "jpg"
        try {
            val file = Files.createDirectories(coversDir).resolve("${UUID.randomUUID()}.$extension")
            try {
                Files.write(file, bytes)
                file.toString()
            } catch (e: IOException) {
                file.deleteIfExists()
                throw e
            }
        } catch (_: IOException) {
            null
        }
    }

    override suspend fun deleteAllExcept(paths: Set<String>): Unit = withContext(Dispatchers.IO) {
        val directory = coversDir
        if (!Files.isDirectory(directory)) return@withContext
        try {
            Files.newDirectoryStream(directory).use { files ->
                files.forEach { file ->
                    if (file.toString() !in paths) file.deleteIfExists()
                }
            }
        } catch (_: IOException) {
            // Best effort: whatever is left is cleared by the next restore.
        }
    }

    /** [path] as a file directly inside the covers directory, or `null` for anything else. */
    private fun insideCovers(path: String): Path? {
        val file = try {
            Paths.get(path).toAbsolutePath().normalize()
        } catch (_: InvalidPathException) {
            return null
        }
        return file.takeIf { it.parent == coversDir && it.isRegularFile() }
    }

    private fun ByteArray.startsWithPngSignature(): Boolean =
        size >= PNG_SIGNATURE.size && PNG_SIGNATURE.indices.all { this[it] == PNG_SIGNATURE[it] }

    /** Draws the image into a new one no larger than [MAX_DIMENSION_PX] on its longest side. */
    private fun BufferedImage.scaledToFit(hasAlpha: Boolean): BufferedImage {
        val longestSide = max(width, height)
        val scale = if (longestSide <= MAX_DIMENSION_PX) 1.0 else MAX_DIMENSION_PX.toDouble() / longestSide
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        val type = if (hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        return BufferedImage(targetWidth, targetHeight, type).also { target ->
            val graphics = target.createGraphics()
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                graphics.drawImage(this, 0, 0, targetWidth, targetHeight, null)
            } finally {
                graphics.dispose()
            }
        }
    }

    /** `false` when the JDK has no JPEG writer, which the caller treats like any other failed write. */
    private fun writeJpeg(image: BufferedImage, file: Path): Boolean {
        val writer = ImageIO.getImageWritersByFormatName("jpg").asSequence().firstOrNull() ?: return false
        try {
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = JPEG_QUALITY
            }
            ImageIO.createImageOutputStream(file.toFile()).use { output ->
                writer.output = output
                writer.write(null, IIOImage(image, null, null), params)
            }
            return true
        } finally {
            writer.dispose()
        }
    }
}
