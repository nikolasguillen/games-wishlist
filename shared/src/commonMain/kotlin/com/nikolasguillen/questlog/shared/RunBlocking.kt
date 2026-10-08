package com.nikolasguillen.questlog.shared

import kotlinx.coroutines.CoroutineScope

/**
 * `runBlocking`, which `commonMain` cannot see. Only for code that has to answer synchronously and is already
 * on a background thread, such as the database's one-time seed callback.
 */
internal expect fun <T> runBlockingCompat(block: suspend CoroutineScope.() -> T): T
