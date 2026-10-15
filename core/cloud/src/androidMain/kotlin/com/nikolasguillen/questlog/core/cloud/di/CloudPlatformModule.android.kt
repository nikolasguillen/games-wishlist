package com.nikolasguillen.questlog.core.cloud.di

import android.app.Application
import com.nikolasguillen.questlog.core.cloud.AndroidProviderAuth
import com.nikolasguillen.questlog.core.cloud.CurrentActivityTracker
import com.nikolasguillen.questlog.core.cloud.PlatformProviderAuth
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val cloudPlatformModule = module {
    // Created at start so the lifecycle callbacks are registered before the first Activity resumes.
    single(createdAtStart = true) {
        CurrentActivityTracker().also { (androidContext() as Application).registerActivityLifecycleCallbacks(it) }
    }
    single<PlatformProviderAuth> {
        AndroidProviderAuth(androidContext(), get(), getProperty("google_web_client_id"))
    }
}
