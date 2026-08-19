package com.example.gameswishlist.core.network.di

import android.os.SystemClock
import com.example.gameswishlist.core.network.BuildConfig
import com.example.gameswishlist.core.network.ElapsedRealtimeSource
import com.example.gameswishlist.core.network.IgdbApiService
import com.example.gameswishlist.core.network.IgdbAuthManager
import com.example.gameswishlist.core.network.IgdbAuthService
import com.example.gameswishlist.core.network.IgdbHttpErrorInterceptor
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    fun provideElapsedRealtimeSource(): ElapsedRealtimeSource {
        return ElapsedRealtimeSource { SystemClock.elapsedRealtime() }
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

    @Provides
    @Singleton
    fun provideAuthInterceptor(authManager: Lazy<IgdbAuthManager>): Interceptor {
        return Interceptor { chain ->
            val token = runBlocking { authManager.get().getAccessToken() }
            val request = chain.request().newBuilder()
                .addHeader("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                .apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Outermost on purpose: the logging interceptor below sees the failed response and dumps it
            // before this one throws it away in favour of an IgdbHttpException.
            .addInterceptor(IgdbHttpErrorInterceptor())
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
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
