package com.nikolasguillen.questlog.core.domain.usecase.settings

import com.nikolasguillen.questlog.core.domain.settings.AppearancePreferenceStore
import com.nikolasguillen.questlog.core.model.AppearanceMode
import javax.inject.Inject

/** The single entry point behind the Settings screen's Appearance selector. */
class SetAppearanceModeUseCase @Inject constructor(
    private val appearancePreferenceStore: AppearancePreferenceStore
) {
    suspend operator fun invoke(mode: AppearanceMode) = appearancePreferenceStore.setAppearanceMode(mode)
}
