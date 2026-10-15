package com.nikolasguillen.questlog.core.cloud

import kotlin.time.Duration.Companion.seconds

/** How long one Firebase call may take before it becomes a `CloudTimeoutException`. */
internal val CLOUD_CALL_TIMEOUT = 30.seconds
