package com.nikolasguillen.questlog.core.data.backup

import kotlin.io.encoding.Base64

/** A backup stream that cannot be put back together: a bad length, or text that is not base64. */
internal class BackupUnreadableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Base64 text cut into parts of at most [maxChars] characters, so each fits a Firestore document. Base64 is ASCII,
 * so a character is a byte and cutting anywhere is safe. An empty array has no parts.
 */
internal fun ByteArray.toParts(maxChars: Int = PART_MAX_CHARS): List<String> =
    Base64.encode(this).chunked(maxChars)

/**
 * The inverse of [toParts]. Throws [BackupUnreadableException] when the text is not base64 or when the decoded length
 * is not [expectedLength], which is how a part that went missing or was cut short shows up.
 */
internal fun List<String>.joinParts(expectedLength: Long): ByteArray {
    val bytes = try {
        Base64.decode(joinToString(separator = ""))
    } catch (e: IllegalArgumentException) {
        throw BackupUnreadableException("The backup text is not valid base64", e)
    }
    if (bytes.size.toLong() != expectedLength) {
        throw BackupUnreadableException("Expected $expectedLength bytes but decoded ${bytes.size}")
    }
    return bytes
}
