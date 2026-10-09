package com.nikolasguillen.questlog.core.data.translation

/**
 * Apple's on-device language model, as the iOS translator sees it.
 *
 * Implemented in Swift by `iosApp` (`FoundationModelsBridge`) and handed to Kotlin through `MainViewController`:
 * FoundationModels is a Swift-only framework with no Objective-C surface, so Kotlin/Native cannot call it.
 * Public only because Swift has to see it to implement it.
 *
 * Nothing here is `suspend`: a `suspend` member implemented in Swift has no cancellation and crashes on a thrown
 * Swift error, so [generate] reports through a callback and returns a handle that cancels the Swift task.
 *
 * The language comes from here too, because Swift reads it the way the model's own language check does — from
 * the user's preferred languages. The app's locale would not do: the app ships English resources only, and on iOS
 * the locale's language follows the app's localization, so it would always be English.
 */
interface AppleLanguageModelBridge {

    /** The user's first preferred language as a BCP 47 tag, such as `es-ES`. Also the cache key. */
    fun preferredLanguageTag(): String

    /** [preferredLanguageTag]'s name in English, such as `Spanish (Spain)`, for the prompt. */
    fun preferredLanguageEnglishName(): String

    /** Whether the model can translate into [preferredLanguageTag] right now. */
    fun availability(): AppleLanguageModelAvailability

    /**
     * Runs one generation in a fresh session. [onResult] is called exactly once, from any thread, with the
     * model's text, or `null` on any failure: a guardrail refusal, a prompt too long for the context window, an
     * unsupported language, or cancellation through the returned handle.
     */
    fun generate(
        instructions: String,
        prompt: String,
        onResult: (String?) -> Unit
    ): AppleLanguageModelGeneration
}
