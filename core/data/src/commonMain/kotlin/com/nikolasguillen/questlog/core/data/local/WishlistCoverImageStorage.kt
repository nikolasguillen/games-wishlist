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

    /**
     * Writes bytes that [persist] produced on some device (so they are already downscaled) as a new file in the covers
     * directory and returns its path, or `null` when it could not be written. The extension is sniffed from the magic
     * bytes: PNG when they start `89 50 4E 47`, JPEG otherwise. Nothing is re-encoded.
     */
    suspend fun import(bytes: ByteArray): String?

    /**
     * Deletes every file in the covers directory that is not in [paths]. A restore that was killed after importing its
     * covers and before the database transaction leaves such orphans, and this clears them before the next one.
     */
    suspend fun deleteAllExcept(paths: Set<String>)
}
