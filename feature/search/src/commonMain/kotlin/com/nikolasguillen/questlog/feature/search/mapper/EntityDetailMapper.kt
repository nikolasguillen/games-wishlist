package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.domain.translation.TranslationSubject
import com.nikolasguillen.questlog.core.model.CharacterProfile
import com.nikolasguillen.questlog.core.model.DeveloperProfile
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.EntityDetailSubject
import com.nikolasguillen.questlog.feature.search.model.EntityHeaderUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.entity_founded_year
import com.nikolasguillen.questlog.feature.search.resources.entity_game_count

internal fun CharacterProfile.toHeader(): EntityHeaderUiModel =
    EntityHeaderUiModel(name = name, imageUrl = imageUrl, foundedYearText = null)

internal fun DeveloperProfile.toHeader(): EntityHeaderUiModel = EntityHeaderUiModel(
    name = name,
    imageUrl = logoUrl,
    foundedYearText = foundedYear?.let { UiText.StringResource(Res.string.entity_founded_year, it) }
)

/** The "N games" chip, with the singular for exactly one. */
internal fun Int.toGameCountText(): UiText = UiText.PluralResource(Res.plurals.entity_game_count, this, this)

internal fun EntityDetailSubject.toTranslationSubject(): TranslationSubject = when (this) {
    is EntityDetailSubject.Character -> TranslationSubject.Character(id)
    is EntityDetailSubject.Developer -> TranslationSubject.Developer(id)
}
