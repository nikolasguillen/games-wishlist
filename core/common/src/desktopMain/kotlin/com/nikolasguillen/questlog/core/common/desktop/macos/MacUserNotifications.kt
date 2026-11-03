package com.nikolasguillen.questlog.core.common.desktop.macos

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationRequest
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotifications
import com.nikolasguillen.questlog.core.common.desktop.DesktopPendingNotification
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.number
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Instant

private const val LIBRARY_NAME = "libQuestLogNotifications.dylib"
private const val RESOURCES_DIRECTORY_PROPERTY = "compose.application.resources.dir"

/**
 * [DesktopNotifications] over `UserNotifications`, reached through the Swift library that sits in the app resources.
 * Without the library, or without an app bundle (a plain `run`), every call reports "unsupported" or refuses.
 */
internal class MacUserNotifications(
    private val library: QuestLogNotificationsLibrary? = loadLibrary()
) : DesktopNotifications {

    // JNA holds a callback weakly, so one passed to the native side must stay reachable until it has been called.
    private val liveCallbacks = ConcurrentHashMap.newKeySet<Callback>()

    override suspend fun status(): DesktopNotificationStatus = guarded(DesktopNotificationStatus.UNSUPPORTED) {
        val native = library ?: return@guarded DesktopNotificationStatus.UNSUPPORTED
        when (native.ql_authorization_status()) {
            0 -> DesktopNotificationStatus.NOT_DETERMINED
            1 -> DesktopNotificationStatus.AUTHORIZED
            2 -> DesktopNotificationStatus.DENIED
            else -> DesktopNotificationStatus.UNSUPPORTED
        }
    }

    override suspend fun requestAuthorization(): Boolean = guarded(false) {
        val native = library ?: return@guarded false
        awaitResult { done -> native.ql_request_authorization(done) }
    }

    override suspend fun schedule(request: DesktopNotificationRequest): Boolean = guarded(false) {
        val native = library ?: return@guarded false
        val at = request.fireAt
        awaitResult { done ->
            if (at == null) {
                native.ql_add_immediate(request.gameId, request.title, request.body, done)
            } else {
                native.ql_add_calendar(
                    request.gameId, request.title, request.body,
                    at.year, at.month.number, at.day, at.hour, at.minute,
                    done
                )
            }
        }
    }

    override suspend fun pending(): List<DesktopPendingNotification> = guarded(emptyList()) {
        val native = library ?: return@guarded emptyList()
        awaitCallback<List<DesktopPendingNotification>> { resume ->
            QuestLogNotificationsLibrary.PendingCallback { count, ids, epochSeconds ->
                // The arrays die with this call, so they are copied here.
                val gameIds = if (ids == null) IntArray(0) else ids.getIntArray(0, count)
                val seconds = if (epochSeconds == null) DoubleArray(0) else epochSeconds.getDoubleArray(0, count)
                resume(
                    gameIds.mapIndexed { index, id ->
                        DesktopPendingNotification(id, seconds.getOrNull(index)?.toInstantOrNull())
                    }
                )
            }.also { native.ql_pending(it) }
        }
    }

    override suspend fun remove(gameIds: Collection<Int>) {
        if (gameIds.isEmpty()) return
        guarded(Unit) { library?.ql_remove(gameIds.toIntArray(), gameIds.size) }
    }

    override fun setClickHandler(onClick: (gameId: Int) -> Unit) {
        val native = library ?: return
        // Kept for the life of the process: the system may call it at any time.
        val callback = QuestLogNotificationsLibrary.ClickCallback { gameId -> runCatching { onClick(gameId) } }
        liveCallbacks += callback
        runCatching { native.ql_set_click_handler(callback) }
    }

    private suspend fun awaitResult(start: (QuestLogNotificationsLibrary.ResultCallback) -> Unit): Boolean =
        awaitCallback<Boolean> { resume ->
            QuestLogNotificationsLibrary.ResultCallback { value -> resume(value == 1) }.also(start)
        }

    /** Suspends until the callback built by [register] reports once. It is not collected before that. */
    private suspend fun <T> awaitCallback(register: ((T) -> Unit) -> Callback): T =
        suspendCancellableCoroutine { continuation ->
            val answered = AtomicBoolean(false)
            var callback: Callback? = null
            callback = register { value ->
                answered.set(true)
                callback?.let(liveCallbacks::remove)
                if (continuation.isActive) continuation.resume(value)
            }
            // An answer that came before register returned (no app bundle) has nothing left to keep alive.
            if (!answered.get()) liveCallbacks += callback
        }

    private suspend fun <T> guarded(fallback: T, block: suspend () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            fallback
        }
    }

    private fun Double.toInstantOrNull(): Instant? =
        if (isNaN()) null else Instant.fromEpochMilliseconds((this * 1000).toLong())
}

private fun loadLibrary(): QuestLogNotificationsLibrary? = runCatching {
    val directory = System.getProperty(RESOURCES_DIRECTORY_PROPERTY) ?: return null
    val file = File(directory, LIBRARY_NAME).takeIf { it.isFile } ?: return null
    Native.load(
        file.absolutePath,
        QuestLogNotificationsLibrary::class.java,
        mapOf(Library.OPTION_STRING_ENCODING to "UTF-8")
    )
}.getOrNull()
