package com.nikolasguillen.questlog.core.network.di

import android.os.SystemClock
import com.nikolasguillen.questlog.core.network.BuildConfig
import com.nikolasguillen.questlog.core.network.ElapsedRealtimeSource
import com.nikolasguillen.questlog.core.network.IgdbApiService
import com.nikolasguillen.questlog.core.network.IgdbAuthManager
import com.nikolasguillen.questlog.core.network.IgdbAuthService
import com.nikolasguillen.questlog.core.network.IgdbHttpErrorInterceptor
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val networkModule = module {
    single { Moshi.Builder().build() }

    single { ElapsedRealtimeSource { SystemClock.elapsedRealtime() } }

    // Full request/response logging is debug-only: at BODY level OkHttp also dumps the headers, which carry
    // the IGDB `Client-ID` and the `Authorization: Bearer` token. On release builds the interceptor stays
    // wired but silent.
    single {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
    }

    single<Interceptor> {
        // Resolved at call time, not at construction: the manager is only needed once a request goes out.
        val authManager: IgdbAuthManager by inject()
        Interceptor { chain ->
            val token = runBlocking { authManager.getAccessToken() }
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

    single {
        OkHttpClient.Builder()
            // Outermost on purpose: the logging interceptor below sees the failed response and dumps it
            // before this one throws it away in favour of an IgdbHttpException.
            .addInterceptor(IgdbHttpErrorInterceptor())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .addInterceptor(get<Interceptor>())
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://api.igdb.com/v4/")
            .client(get<OkHttpClient>())
            .addConverterFactory(MoshiConverterFactory.create(get<Moshi>()))
            .build()
    }

    single<IgdbAuthService> {
        Retrofit.Builder()
            .baseUrl("https://id.twitch.tv/")
            .addConverterFactory(MoshiConverterFactory.create(get<Moshi>()))
            .build()
            .create(IgdbAuthService::class.java)
    }

    single<IgdbApiService> { get<Retrofit>().create(IgdbApiService::class.java) }

    single { IgdbAuthManager(get(), get()) }
}
