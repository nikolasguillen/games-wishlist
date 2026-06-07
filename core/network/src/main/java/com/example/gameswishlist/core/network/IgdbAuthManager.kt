package com.example.gameswishlist.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class IgdbAuthManager @Inject constructor(
    private val authService: IgdbAuthService
) {
    private var accessToken: String? = null
    private val mutex = Mutex()

    suspend fun getAccessToken(): String? = mutex.withLock {
        if (accessToken != null) return@withLock accessToken
        
        try {
            val response = authService.getAccessToken(
                clientId = BuildConfig.IGDB_CLIENT_ID,
                clientSecret = BuildConfig.IGDB_CLIENT_SECRET
            )
            accessToken = response.accessToken
            accessToken
        } catch (e: Exception) {
            null
        }
    }
}
