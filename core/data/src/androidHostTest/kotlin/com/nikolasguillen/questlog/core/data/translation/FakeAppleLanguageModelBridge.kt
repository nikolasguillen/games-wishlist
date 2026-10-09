package com.nikolasguillen.questlog.core.data.translation

/**
 * Stands in for the Swift side of [AppleLanguageModelBridge]. [respond] set to `false` leaves the callback
 * uncalled, the way a generation still running looks to the translator.
 */
class FakeAppleLanguageModelBridge : AppleLanguageModelBridge {

    var languageTag = "it-IT"
    var languageName = "Italian"
    var availability = AppleLanguageModelAvailability.AVAILABLE
    var result: String? = "Una descrizione."
    var respond = true

    /** Answers served by [availability] one call at a time, before it falls back to the [availability] field. */
    val availabilitySequence = ArrayDeque<AppleLanguageModelAvailability>()

    var availabilityCalls = 0
        private set
    var cancelled = false
        private set
    val requests = mutableListOf<Request>()

    data class Request(val instructions: String, val prompt: String)

    override fun preferredLanguageTag(): String = languageTag

    override fun preferredLanguageEnglishName(): String = languageName

    override fun availability(): AppleLanguageModelAvailability {
        availabilityCalls++
        return availabilitySequence.removeFirstOrNull() ?: availability
    }

    override fun generate(
        instructions: String,
        prompt: String,
        onResult: (String?) -> Unit
    ): AppleLanguageModelGeneration {
        requests += Request(instructions, prompt)
        if (respond) onResult(result)
        return object : AppleLanguageModelGeneration {
            override fun cancel() {
                cancelled = true
            }
        }
    }
}
