package com.nikolasguillen.questlog.di

import android.content.Context
import androidx.work.WorkerParameters
import com.nikolasguillen.questlog.core.common.di.commonPlatformModule
import com.nikolasguillen.questlog.core.data.di.dataModule
import com.nikolasguillen.questlog.core.data.di.dataPlatformModule
import com.nikolasguillen.questlog.core.database.di.databaseModule
import com.nikolasguillen.questlog.core.database.di.databasePlatformModule
import com.nikolasguillen.questlog.core.domain.di.domainModule
import com.nikolasguillen.questlog.core.network.di.networkModule
import com.nikolasguillen.questlog.core.network.di.networkPlatformModule
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Checks that every dependency of every registered class is declared in some module, so a missing binding
 * fails here instead of the first time a screen opens. The route arguments (a game id, a list id) and the
 * Android framework objects are supplied at runtime, so they are declared as extra types.
 */
class KoinGraphTest {

    @Test
    fun `every dependency in the graph is declared`() {
        // verify() checks one module at a time, and these depend on each other, so they are checked as one.
        module {
            includes(
                commonPlatformModule, domainModule, networkModule, networkPlatformModule, databaseModule,
                databasePlatformModule, dataModule, dataPlatformModule, interimModule, viewModelModule
            )
        }.verify(extraTypes = listOf(
            Context::class, WorkerParameters::class, Int::class, Long::class, Boolean::class
        ))
    }
}
