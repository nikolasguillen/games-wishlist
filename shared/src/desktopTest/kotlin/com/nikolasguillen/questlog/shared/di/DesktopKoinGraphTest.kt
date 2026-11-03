package com.nikolasguillen.questlog.shared.di

import com.nikolasguillen.questlog.core.data.notification.ReminderSyncTriggers
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier
import com.nikolasguillen.questlog.feature.search.model.EntityDetailSubject
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.verify.verify
import java.nio.file.Path

/**
 * The desktop counterpart of the Android `KoinGraphTest`: every dependency of every registered class is declared in
 * some module, so a missing binding fails here instead of the first time a screen opens. The route arguments are
 * supplied at runtime, so they are declared as extra types; there is no Android framework object on desktop.
 *
 * The modules are the ones [initKoin] starts, taken from the same list.
 */
class DesktopKoinGraphTest {

    @Test
    fun `every dependency in the graph is declared`() {
        // verify() checks one module at a time, and these depend on each other, so they are checked as one.
        module {
            includes(allModules(isDebugBuild = false))
        }.verify(
            extraTypes = listOf(
                Int::class, Long::class, Boolean::class, EntityDetailSubject::class,
                // Only the Android worker delivers a notification itself, through DeliverReleaseNotificationUseCase;
                // here the operating system posts it, so nothing binds a notifier and nothing resolves that use case.
                ReleaseNotifier::class,
                // Bound by startQuestLogDesktop, because the permission state it listens to lives in :core:ui.
                ReminderSyncTriggers::class,
                // The signed-in session's directory is passed as a Koin property and turned into a path in the module.
                Path::class
            )
        )
    }
}
