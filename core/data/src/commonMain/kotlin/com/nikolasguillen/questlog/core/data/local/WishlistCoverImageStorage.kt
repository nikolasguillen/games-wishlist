package com.nikolasguillen.questlog.core.data.local

/**
 * Keeps a wishlist's custom cover image in app-private storage, so it outlives the picker's own grant on
 * the source image. Each platform decodes, downscales and writes the file its own way.
 */
internal interface WishlistCoverImageStorage {

    /**
     * Copies the picked image into app storage and returns the stable absolute path to save alongside the
     * list, or `null` when it could not be read or written. [source] is an opaque reference that the same
     * platform's picker produced (on Android, a content URI string).
     */
    suspend fun persist(source: String): String?

    /**
     * Removes a cover previously returned by [persist]. Paths outside the covers directory are
     * ignored, so a stale or hand-edited value can never delete an unrelated file.
     */
    suspend fun delete(path: String)

    /**
     * The stored bytes of a cover previously returned by [persist], or `null` when the file is missing or unreadable. Like [delete], it only touches files inside the covers directory, so a stale or hand-edited
     * path cannot read an unrelated file into a backup.
     */
    suspend fun read(path: String): ByteArray?
}
