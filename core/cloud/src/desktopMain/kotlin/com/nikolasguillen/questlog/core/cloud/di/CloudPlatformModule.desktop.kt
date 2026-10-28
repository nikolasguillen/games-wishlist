package com.nikolasguillen.questlog.core.cloud.di

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.CloudBackupStore
import com.nikolasguillen.questlog.core.cloud.DesktopCloudCredentials
import com.nikolasguillen.questlog.core.cloud.DesktopCloudSessionStore
import com.nikolasguillen.questlog.core.cloud.GoogleIdTokenSource
import com.nikolasguillen.questlog.core.cloud.GoogleInstalledAppSignIn
import com.nikolasguillen.questlog.core.cloud.IdTokenSupplier
import com.nikolasguillen.questlog.core.cloud.IdentityToolkitClient
import com.nikolasguillen.questlog.core.cloud.RestBackupStore
import com.nikolasguillen.questlog.core.cloud.RestCloudAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import java.nio.file.Paths

/** The Koin property that holds the directory the signed-in session is kept in; the app sets it (it knows the path). */
const val CLOUD_DATA_DIRECTORY_PROPERTY = "cloud_data_directory"

// Qualified: `:core:network` binds its own HttpClient, which must not be taken for this one.
private val cloudHttpClient = named("cloud-http-client")

actual val cloudPlatformModule = module {
    single(cloudHttpClient) { HttpClient(OkHttp) }
    single { DesktopCloudSessionStore(Paths.get(getProperty<String>(CLOUD_DATA_DIRECTORY_PROPERTY))) }
    single { IdentityToolkitClient(get(cloudHttpClient), DesktopCloudCredentials.FIREBASE_WEB_API_KEY) }
    single<GoogleIdTokenSource> {
        GoogleInstalledAppSignIn(
            httpClient = get(cloudHttpClient),
            clientId = DesktopCloudCredentials.GOOGLE_DESKTOP_CLIENT_ID,
            clientSecret = DesktopCloudCredentials.GOOGLE_DESKTOP_CLIENT_SECRET
        )
    }
    single { RestCloudAuth(identity = get(), google = get(), sessions = get()) } binds
        arrayOf(CloudAuth::class, IdTokenSupplier::class)
    single<CloudBackupStore> {
        RestBackupStore(get(cloudHttpClient), DesktopCloudCredentials.FIREBASE_PROJECT_ID, tokens = get())
    }
}
