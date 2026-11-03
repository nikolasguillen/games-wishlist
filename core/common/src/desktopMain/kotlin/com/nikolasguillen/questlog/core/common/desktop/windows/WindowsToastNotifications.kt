package com.nikolasguillen.questlog.core.common.desktop.windows

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationRequest
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotifications
import com.nikolasguillen.questlog.core.common.desktop.DesktopPendingNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.concurrent.Executors

private const val SETTING_ENABLED = 0

/**
 * [DesktopNotifications] over Windows scheduled toasts. Windows delivers a scheduled toast at its time with no process
 * running and keeps it across restarts, which is what makes a release reminder arrive with the app closed.
 *
 * Every WinRT call runs on one thread that is initialised for COM once; the objects are cheap, so each call makes and
 * releases its own. Nothing throws except `CancellationException`: a failed call is `false`, an empty list or
 * [DesktopNotificationStatus.UNSUPPORTED], and the next sync tries again.
 */
internal class WindowsToastNotifications : DesktopNotifications {

    private val dispatcher = Executors.newSingleThreadExecutor { task ->
        Thread(task, "questlog-winrt").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private var notifier: ToastNotifierBinding? = null

    init {
        // The process id has to be set before the first window exists, so the registration happens at creation.
        AumidRegistration.register()
    }

    override suspend fun status(): DesktopNotificationStatus = guarded(DesktopNotificationStatus.UNSUPPORTED) {
        if (binding().setting() == SETTING_ENABLED) DesktopNotificationStatus.AUTHORIZED else DesktopNotificationStatus.DENIED
    }

    // Windows has no prompt: notifications are on or off in Windows Settings.
    override suspend fun requestAuthorization(): Boolean = status() == DesktopNotificationStatus.AUTHORIZED

    override suspend fun schedule(request: DesktopNotificationRequest): Boolean = guarded(false) {
        val binding = binding()
        if (binding.setting() != SETTING_ENABLED) return@guarded false
        val xml = ToastXml.build(request.gameId, request.title, request.body)
        val fireAt = request.fireAt
        if (fireAt == null) {
            binding.show(xml)
        } else {
            // A floating wall-clock time, converted with the zone it is armed in. Every full sync re-arms.
            val instant = fireAt.toInstant(TimeZone.currentSystemDefault())
            // A scheduled toast replaces the one with the same id only if that one is removed first.
            binding.remove(setOf(ToastXml.toastId(request.gameId)))
            binding.schedule(ToastXml.toastId(request.gameId), xml, ToastXml.toTicks(instant))
        }
        true
    }

    override suspend fun pending(): List<DesktopPendingNotification> = guarded(emptyList()) {
        binding().scheduled().mapNotNull { toast ->
            ToastXml.gameIdOf(toast.id)?.let { DesktopPendingNotification(it, ToastXml.fromTicks(toast.ticks)) }
        }
    }

    override suspend fun remove(gameIds: Collection<Int>) {
        if (gameIds.isEmpty()) return
        guarded(Unit) { binding().remove(gameIds.map(ToastXml::toastId).toSet()) }
    }

    // The click handler arrives with the activator (specs/020-desktop-app, T072 and T073).
    override fun setClickHandler(onClick: (gameId: Int) -> Unit) = Unit

    private suspend fun <T> guarded(fallback: T, block: () -> T): T = withContext(dispatcher) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            fallback
        }
    }

    /** The notifier of this thread, made on first use after COM is initialised. */
    private fun binding(): ToastNotifierBinding = notifier ?: run {
        Combase.instance.RoInitialize(Combase.RO_INIT_MULTITHREADED) // S_FALSE when already initialised: fine.
        ToastNotifierBinding(APP_USER_MODEL_ID).also { notifier = it }
    }
}
