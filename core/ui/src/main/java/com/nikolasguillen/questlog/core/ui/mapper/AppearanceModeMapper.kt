package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.AppearanceMode
import com.nikolasguillen.questlog.core.ui.R
import com.nikolasguillen.questlog.core.ui.model.UiText

fun AppearanceMode.toLabelUiText(): UiText {
    val resId = when (this) {
        AppearanceMode.LIGHT -> R.string.appearance_light
        AppearanceMode.DARK -> R.string.appearance_dark
        AppearanceMode.SYSTEM -> R.string.appearance_system
    }
    return UiText.StringResource(resId)
}
