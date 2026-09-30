package com.nikolasguillen.questlog.core.domain.usecase.settings

import com.nikolasguillen.questlog.core.domain.settings.AppearancePreferenceStore
import com.nikolasguillen.questlog.core.model.AppearanceMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The user's current appearance selection, and every subsequent change. */
class GetAppearanceModeUseCase @Inject constructor(
    private val appearancePreferenceStore: AppearancePreferenceStore
) {
    operator fun invoke(): Flow<AppearanceMode> = appearancePreferenceStore.observeAppearanceMode()
}
