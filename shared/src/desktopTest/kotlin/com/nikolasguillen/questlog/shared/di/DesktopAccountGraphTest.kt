package com.nikolasguillen.questlog.shared.di

import com.nikolasguillen.questlog.core.domain.usecase.account.ObserveAccountUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.koin.dsl.koinApplication

/**
 * Settings reads the account as soon as it opens. On desktop there is no Firebase app to ask, so the account must come
 * from a binding that works without one: resolving and reading it must not throw.
 */
class DesktopAccountGraphTest {

    @Test
    fun `the account can be observed without a Firebase app`() {
        val koin = koinApplication { modules(allModules(isDebugBuild = false)) }.koin
        val account = runBlocking { koin.get<ObserveAccountUseCase>()().first() }
        assertNull(account)
    }
}
