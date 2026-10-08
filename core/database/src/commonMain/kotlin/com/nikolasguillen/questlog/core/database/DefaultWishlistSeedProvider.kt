package com.nikolasguillen.questlog.core.database

/**
 * Supplies the [DefaultWishlistSeed] at the moment the database is first created, not when the app starts: the
 * words come from resources, and reading them is only worth doing once per install.
 */
fun interface DefaultWishlistSeedProvider {
    fun provide(): DefaultWishlistSeed
}
