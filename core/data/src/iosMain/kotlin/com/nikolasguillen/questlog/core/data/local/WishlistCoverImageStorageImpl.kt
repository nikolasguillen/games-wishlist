package com.nikolasguillen.questlog.core.data.local

import com.nikolasguillen.questlog.core.common.applicationSupportDirectory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import kotlin.math.max
import kotlin.math.roundToInt

private const val COVERS_DIR_NAME = "wishlist_covers"

// Longest side in pixels, the same cap as on Android: enough for the full-bleed header of the wishlist detail
// screen without storing camera-resolution originals.
private const val MAX_DIMENSION_PX = 1440.0
private const val JPEG_QUALITY = 0.85

/**
 * iOS implementation of [WishlistCoverImageStorage]. The picker hands over a temporary file; this decodes it,
 * downscales it to [MAX_DIMENSION_PX], writes it into `Application Support/wishlist_covers`, and deletes the
 * temporary file, so a cover outlives the picker's own copy.
 *
 * Re-encoded so the extension always matches the content: PNG when the image has transparency, JPEG otherwise.
 */
@OptIn(ExperimentalForeignApi::class)
class WishlistCoverImageStorageImpl : WishlistCoverImageStorage {

    private val coversDir: String get() = "${applicationSupportDirectory()}/$COVERS_DIR_NAME"

    override suspend fun persist(source: String): String? = withContext(Dispatchers.IO) {
        try {
            val image = UIImage.imageWithContentsOfFile(source) ?: return@withContext null
            val hasAlpha = image.hasAlpha()
            val scaled = image.scaledToFit(hasAlpha) ?: return@withContext null
            val data = (if (hasAlpha) UIImagePNGRepresentation(scaled) else UIImageJPEGRepresentation(scaled, JPEG_QUALITY))
                ?: return@withContext null

            val fileManager = NSFileManager.defaultManager
            fileManager.createDirectoryAtPath(coversDir, withIntermediateDirectories = true, attributes = null, error = null)
            val path = "$coversDir/${NSUUID().UUIDString}.${if (hasAlpha) "png" else "jpg"}"
            if (data.writeToFile(path, atomically = true)) path else null
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(source, error = null)
        }
    }

    override suspend fun delete(path: String): Unit = withContext(Dispatchers.IO) {
        // Only files this class wrote: a stale or hand-edited value can never delete an unrelated one.
        if (path.substringBeforeLast('/') == coversDir) {
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        }
    }

    private fun UIImage.hasAlpha(): Boolean {
        val cgImage = CGImage ?: return false
        return when (CGImageGetAlphaInfo(cgImage)) {
            CGImageAlphaInfo.kCGImageAlphaNone,
            CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst,
            CGImageAlphaInfo.kCGImageAlphaNoneSkipLast -> false
            else -> true
        }
    }

    /**
     * Draws the image into a new one no larger than [MAX_DIMENSION_PX] on its longest side. Drawing, rather than
     * cropping the pixel data, applies the EXIF orientation as well.
     */
    private fun UIImage.scaledToFit(hasAlpha: Boolean): UIImage? {
        val (width, height) = size.useContents { width to height }
        val longestSide = max(width, height)
        val scale = if (longestSide <= MAX_DIMENSION_PX) 1.0 else MAX_DIMENSION_PX / longestSide
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1).toDouble()
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1).toDouble()

        val format = UIGraphicsImageRendererFormat.preferredFormat().apply {
            // One point per pixel, so the cap above is in pixels whatever the screen's own scale.
            setScale(1.0)
            setOpaque(!hasAlpha)
        }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth, targetHeight), format = format)
        return renderer.imageWithActions { drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight)) }
    }
}
