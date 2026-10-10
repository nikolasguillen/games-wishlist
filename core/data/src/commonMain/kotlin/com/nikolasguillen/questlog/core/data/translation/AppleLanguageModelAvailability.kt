package com.nikolasguillen.questlog.core.data.translation

/**
 * `SystemLanguageModel.availability` from Apple's FoundationModels framework, folded together with the check that
 * the model can write the user's preferred language, so Kotlin never sees a Swift type.
 */
enum class AppleLanguageModelAvailability {
    /** Ready, and the preferred language is supported. */
    AVAILABLE,

    /** `.unavailable(.modelNotReady)`: the system is still downloading or preparing the model. */
    NOT_READY,

    /**
     * Any other reason: below iOS 26, an ineligible device, Apple Intelligence turned off, or a model that reports
     * itself available while having no context window, so it could not generate anything.
     */
    UNAVAILABLE,

    /** The model is ready but cannot write the preferred language. */
    LANGUAGE_UNSUPPORTED
}
