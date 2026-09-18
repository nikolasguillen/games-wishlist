package com.example.gameswishlist.core.data.translation

import java.util.Locale

/** Built in English regardless of the target, so the instructions themselves stay unambiguous. */
internal fun buildTranslationPrompt(description: String): String {
    val targetLanguage = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH)
    return """
        You are translating text for a video game catalogue app.
        Translate the text between the <text> tags from English into $targetLanguage.
        Keep game titles, character names, studio names and platform names untranslated.
        Preserve the paragraph structure.

        <text>
        $description
        </text>

        Output the translated text only: no tags, no labels, no quotes, no commentary.
    """.trimIndent()
}
