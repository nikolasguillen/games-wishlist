package com.nikolasguillen.questlog.core.data.translation

/**
 * The rules half of the translation prompt, built in English regardless of the target so the instructions
 * themselves stay unambiguous. It is identical across every translation for a given language, which is what
 * lets each engine treat it as the fixed part of the request: Android sends it as Gemini Nano's cached prompt
 * prefix, so the model skips re-processing these tokens on every game, and iOS as the session's instructions.
 *
 * @param targetLanguage The language's English name, such as `Italian`.
 */
internal fun buildTranslationInstructions(targetLanguage: String): String = """
    You are translating text for a video game catalogue app.
    Translate the text between the <text> tags from English into $targetLanguage.
    Keep game titles, character names, studio names and platform names untranslated.
    Preserve the paragraph structure.
    Output the translated text only: no tags, no labels, no quotes, no commentary.
""".trimIndent()

/** The variable half: the description wrapped in the tags [buildTranslationInstructions] refers to. */
internal fun buildTranslationSource(description: String): String = "<text>\n$description\n</text>"
