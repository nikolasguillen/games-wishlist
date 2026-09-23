package com.nikolasguillen.questlog.core.data.translation

import java.util.Locale

/**
 * Built in English regardless of the target, so the instructions themselves stay unambiguous.
 *
 * Split from [buildTranslationPromptSuffix] so the two can be sent as the cached and the dynamic half of
 * the request: this text is identical across every translation in a session, so passing it as the model's
 * prompt prefix lets it skip re-processing these tokens on every game instead of paying for them again.
 */
internal fun buildTranslationPromptPrefix(): String {
    val targetLanguage = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH)
    return """
        You are translating text for a video game catalogue app.
        Translate the text between the <text> tags from English into $targetLanguage.
        Keep game titles, character names, studio names and platform names untranslated.
        Preserve the paragraph structure.
        Output the translated text only: no tags, no labels, no quotes, no commentary.

        <text>

    """.trimIndent()
}

/** The variable half of the prompt — kept out of the cached prefix since it changes on every call. */
internal fun buildTranslationPromptSuffix(description: String): String = "$description\n</text>"
