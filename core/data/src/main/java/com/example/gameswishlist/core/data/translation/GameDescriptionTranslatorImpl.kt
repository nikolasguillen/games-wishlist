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
            return cached.translatedText
        }

        val translated = geminiNanoClient.generate(buildPrompt(description))
        if (translated.isNullOrBlank()) return null

        translationDao.saveTranslation(
            TranslatedDescriptionEntity(
                gameId = gameId,
                languageTag = languageTag,
                sourceHash = sourceHash,
                translatedText = translated
            )
        )
        return translated
    }

    /** Built in English regardless of the target, so the instructions themselves stay unambiguous. */
    private fun buildPrompt(description: String): String {
        val targetLanguage = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH)
        return """
            Translate the following video game description from English into $targetLanguage.
            Keep game titles, character names, studio names and platform names untranslated.
            Preserve the paragraph structure. Do not add any commentary, notes or preamble.
            Reply with the translation only, nothing else.

            Description:
            $description
        """.trimIndent()
    }

    private companion object {
        // Well inside the Prompt API's ~4000-token ceiling; no IGDB summary comes close to this.
        const val MAX_TRANSLATABLE_CHARS = 8000
    }
}
