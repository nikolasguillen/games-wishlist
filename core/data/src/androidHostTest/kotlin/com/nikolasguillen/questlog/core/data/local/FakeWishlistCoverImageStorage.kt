package com.nikolasguillen.questlog.core.data.local

/**
 * A [WishlistCoverImageStorage] over a map of path to bytes; a path that is not in [files] is a missing file.
 * [imported] lists every path [import] created, so a test can see which files a failed restore cleaned up.
 */
internal class FakeWishlistCoverImageStorage(
    val files: MutableMap<String, ByteArray> = mutableMapOf()
) : WishlistCoverImageStorage {
    val imported = mutableListOf<String>()
    val calls = mutableListOf<String>()
    var failImport = false
    private var nextId = 0

    override suspend fun persist(source: String): String? = null

    override suspend fun delete(path: String) {
        calls += "delete:$path"
        files.remove(path)
    }

    override suspend fun read(path: String): ByteArray? = files[path]

    override suspend fun import(bytes: ByteArray): String? {
        calls += "import"
        if (failImport) return null
        val path = "/covers/imported-${nextId++}.jpg"
        files[path] = bytes
        imported += path
        return path
    }

    override suspend fun deleteAllExcept(paths: Set<String>) {
        calls += "deleteAllExcept"
        files.keys.filter { it !in paths }.forEach { files.remove(it) }
    }
}
