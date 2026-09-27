package com.nikolasguillen.questlog.core.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.nikolasguillen.questlog.core.domain.radar.RefreshReleaseDatesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SyncReleaseNotificationsUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the reconcile-after-refresh wiring this worker adds on top of the release-date refresh: a
 * successful refresh must reconcile every release-notification opt-in (FR-007), a failed one must not.
 * `doWork()` is called directly rather than through WorkManager's test harness -- it never reads
 * [Context] or [WorkerParameters], so a relaxed mock of each is enough and no Robolectric/work-testing
 * dependency is needed.
 */
class ReleaseDatesRefreshWorkerTest {

    private val refreshReleaseDatesUseCase = mockk<RefreshReleaseDatesUseCase>()
    private val syncReleaseNotificationsUseCase = mockk<SyncReleaseNotificationsUseCase>(relaxed = true)

    private val worker = ReleaseDatesRefreshWorker(
        context = mockk<Context>(relaxed = true),
        params = mockk<WorkerParameters>(relaxed = true),
        refreshReleaseDatesUseCase = refreshReleaseDatesUseCase,
        syncReleaseNotificationsUseCase = syncReleaseNotificationsUseCase
    )

    @Test
    fun `successful refresh reconciles every opt-in and succeeds`() = runTest {
        coEvery { refreshReleaseDatesUseCase() } returns AppResult.success(Unit)

        val result = worker.doWork()

        coVerify(exactly = 1) { syncReleaseNotificationsUseCase(null) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `failed refresh does not reconcile and retries`() = runTest {
        coEvery { refreshReleaseDatesUseCase() } returns AppResult.failure(RepositoryError.NoNetwork)

        val result = worker.doWork()

        coVerify(exactly = 0) { syncReleaseNotificationsUseCase(any()) }
        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
