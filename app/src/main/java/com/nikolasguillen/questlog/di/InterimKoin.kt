package com.nikolasguillen.questlog.di

import com.nikolasguillen.questlog.BuildConfig
import com.nikolasguillen.questlog.R
import com.nikolasguillen.questlog.core.database.DefaultWishlistSeed
import com.nikolasguillen.questlog.core.network.NetworkConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Bindings that need `:app`'s own `BuildConfig` and have no other home until the shared module exists, which
 * then supplies them from `initKoin`. Deleted with the app root's move to `:shared`.
 */
val interimModule = module {
    single { NetworkConfig(logBodies = BuildConfig.DEBUG) }

    single {
        val context = androidContext()
        DefaultWishlistSeed(
            name = context.getString(R.string.default_wishlist_name),
            description = context.getString(R.string.default_wishlist_description)
        )
    }
}
