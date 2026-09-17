package com.example.gameswishlist.core.data.translation

import com.example.gameswishlist.core.ai.GeminiNanoClient
import com.example.gameswishlist.core.database.dao.TranslationDao
import com.example.gameswishlist.core.database.entity.TranslatedDescriptionEntity
import com.example.gameswishlist.core.domain.translation.GameDescriptionTranslator
import java.util.Locale
import javax.inject.Inject

class GameDescriptionTranslatorImpl @Inject constructor(
    private val geminiNanoClient: GeminiNanoClient,
    private val translationDao: TranslationDao
) : GameDescriptionTranslator {

    override suspend fun isSupported(): Boolean {
        // Cheap check first: no point asking AICore about a language the device is already showing.
        if (Locale.getDefault().language == Locale.ENGLISH.language) return false
        return geminiNanoClient.isModelReady()
    }

    override suspend fun translate(gameId: Int, description: String): String? {
        if (description.isBlank() || description.length > MAX_TRANSLATABLE_CHARS) return null

        val languageTag = Locale.getDefault().toLanguageTag()
        val sourceHash = description.hashCode()

        val cached = translationDao.getTranslation(gameId, languageTag)
        if (cached != null && cached.sourceHash == sourceHash) {
            // Rows written before the prompt fix can still carry a leaked label; sanitize on read too.
            return cached.translatedText.stripTranslationArtifacts()
        }

        val translated = geminiNanoClient.generate(buildPrompt(description))
        if (translated.isNullOrBlank()) return null

        val sanitized = translated.stripTranslationArtifacts()
        translationDao.saveTranslation(
            TranslatedDescriptionEntity(
                gameId = gameId,
                languageTag = languageTag,
                sourceHash = sourceHash,
                translatedText = sanitized
            )
        )
        return sanitized
    }

    /** Built in English regardless of the target, so the instructions themselves stay unambiguous. */
    private fun buildPrompt(description: String): String {
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

    /**
     * A small on-device model will occasionally ignore the output-format instruction regardless of how
     * the prompt is worded, most often by echoing a `Description:`/`Descrizione:`-style label or wrapping
     * the answer in a code fence or the `<text>` tag from the prompt itself.
     */
    private fun String.stripTranslationArtifacts(): String {
        var text = trim()

        if (text.startsWith("```") && text.endsWith("```")) {
            text = text.removePrefix("```").removeSuffix("```")
            text = text.substringAfter("\n", text).trim()
        }

        text = text.removePrefix("<text>").removeSuffix("</text>").trim()

        text = text.replaceFirst(Regex("^\\p{L}{1,20}:\\s*"), "")

        if (text.length >= 2 && text.first() == '"' && text.last() == '"' && text.count { it == '"' } == 2) {
            text = text.substring(1, text.length - 1)
        }

        return text.trim()
    }

    private companion object {
        // Well inside the Prompt API's ~4000-token ceiling; no IGDB summary comes close to this.
        const val MAX_TRANSLATABLE_CHARS = 8000
    }
}
