package com.nikolasguillen.questlog.core.database

/**
 * The text of the wishlist the database creates for itself on first launch. It is the app's to supply,
 * because the words belong in its resources, not in this module.
 */
data class DefaultWishlistSeed(val name: String, val description: String)
