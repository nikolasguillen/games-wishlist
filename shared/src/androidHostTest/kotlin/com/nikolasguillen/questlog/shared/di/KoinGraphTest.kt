package com.nikolasguillen.questlog.shared.di

import android.content.Context
import androidx.work.WorkerParameters
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Checks that every dependency of every registered class is declared in some module, so a missing binding
 * fails here instead of the first time a screen opens. The route arguments (a game id, a list id) and the
 * Android framework objects are supplied at runtime, so they are declared as extra types.
 *
 * The modules are the ones [initKoin] starts, taken from the same list.
 */
class KoinGraphTest {

    @Test
    fun `every dependency in the graph is declared`() {
        // verify() checks one module at a time, and these depend on each other, so they are checked as one.
        module {
            includes(allModules(isDebugBuild = false))
        }.verify(
            extraTypes = listOf(
                Context::class, WorkerParameters::class, Int::class, Long::class, Boolean::class
            )
        )
    }
}
