package com.example.gameswishlist.core.network.di

import com.example.gameswishlist.core.network.BuildConfig
import com.example.gameswishlist.core.network.IgdbApiService
import com.example.gameswishlist.core.network.IgdbAuthManager
import com.example.gameswishlist.core.network.IgdbAuthService
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val HEADER_AUTHORIZATION = "Authorization"
    private const val BEARER_PREFIX = "Bearer "

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    /**
     * Full request/response logging is debug-only: at [HttpLoggingInterceptor.Level.BODY] OkHttp also
     * dumps the headers, which carry the IGDB `Client-ID` and the `Authorization: Bearer` token. On
     * release builds the interceptor stays wired but silent.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Attaches the token up front so the very first request of the process already carries it. A
     * failure to obtain one fails the request instead of sending it unauthenticated, which IGDB would
     * answer with an opaque 401.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(authManager: Lazy<IgdbAuthManager>): Interceptor {
        return Interceptor { chain ->
            val token = runBlocking { authManager.get().getAccessToken() }
            val request = chain.request().newBuilder()
                .addHeader("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                .addHeader(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
            chain.proceed(request)
        }
    }

    /**
     * Covers the tokens IGDB rejects before their `expires_in` elapses — revoked credentials, a
     * server-side reset. OkHttp calls this on a 401, and the request it returns is retried below the
     * application interceptors, so the refreshed header has to be set here as well. Bailing out on a
     * non-null `priorResponse` caps this at one retry per call.
     */
    @Provides
    @Singleton
    fun provideAuthAuthenticator(authManager: Lazy<IgdbAuthManager>): Authenticator {
        return Authenticator { _, response ->
            val staleToken = response.request.header(HEADER_AUTHORIZATION)
                ?.removePrefix(BEARER_PREFIX)
            if (staleToken == null || response.priorResponse != null) return@Authenticator null

            val token = runBlocking { authManager.get().refreshAccessToken(staleToken) }
            response.request.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
        authenticator: Authenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.igdb.com/v4/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideIgdbAuthService(moshi: Moshi): IgdbAuthService {
        return Retrofit.Builder()
            .baseUrl("https://id.twitch.tv/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IgdbAuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideIgdbApiService(retrofit: Retrofit): IgdbApiService {
        return retrofit.create(IgdbApiService::class.java)
    }
}
