package com.nikolasguillen.questlog.shared.di

import com.nikolasguillen.questlog.core.cloud.di.CLOUD_DATA_DIRECTORY_PROPERTY
import com.nikolasguillen.questlog.core.domain.usecase.account.ObserveAccountUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication

/**
 * Settings reads the account as soon as it opens. On desktop there is no Firebase app to ask, so the account must come
 * from the REST implementation: resolving and reading it, signed out, must not throw.
 */
class DesktopAccountGraphTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the account can be observed without a Firebase app`() {
        val koin = koinApplication {
            properties(mapOf(CLOUD_DATA_DIRECTORY_PROPERTY to folder.root.toString()))
            modules(allModules(isDebugBuild = false))
        }.koin
        val account = runBlocking { koin.get<ObserveAccountUseCase>()().first() }
        assertNull(account)
    }
}
