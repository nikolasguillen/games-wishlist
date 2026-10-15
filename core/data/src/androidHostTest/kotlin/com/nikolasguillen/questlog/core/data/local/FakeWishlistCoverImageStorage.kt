package com.nikolasguillen.questlog.core.data.local

/** A [WishlistCoverImageStorage] over a map of path to bytes; a path that is not in [files] is a missing file. */
internal class FakeWishlistCoverImageStorage(
    val files: MutableMap<String, ByteArray> = mutableMapOf()
) : WishlistCoverImageStorage {
    override suspend fun persist(source: String): String? = null

    override suspend fun delete(path: String) {
        files.remove(path)
    }

    override suspend fun read(path: String): ByteArray? = files[path]
}
