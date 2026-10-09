package com.nikolasguillen.questlog.core.data.translation

/** A generation started by [AppleLanguageModelBridge.generate], cancelled when its caller's coroutine is. */
interface AppleLanguageModelGeneration {
    fun cancel()
}
