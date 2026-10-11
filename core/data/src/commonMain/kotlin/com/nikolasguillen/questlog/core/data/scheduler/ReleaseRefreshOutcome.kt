package com.nikolasguillen.questlog.core.data.scheduler

/**
 * How one run of [ReleaseRefreshRunner] ended, other than by cancellation (which is an exception).
 */
internal enum class ReleaseRefreshOutcome {

    /** The use case succeeded and the time was recorded. An empty wishlist counts: there was nothing to do. */
    REFRESHED,

    /**
     * The last success was newer than [RELEASE_DATES_REFRESH_INTERVAL], so nothing ran and no network request was
     * made.
     */
    NOT_DUE,

    /** The use case returned a failure. Nothing was recorded, so the next run tries again. */
    FAILED
}
