package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.AppearanceMode
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.appearance_dark
import com.nikolasguillen.questlog.core.ui.resources.appearance_light
import com.nikolasguillen.questlog.core.ui.resources.appearance_system

fun AppearanceMode.toLabelUiText(): UiText {
    val resId = when (this) {
        AppearanceMode.LIGHT -> Res.string.appearance_light
        AppearanceMode.DARK -> Res.string.appearance_dark
        AppearanceMode.SYSTEM -> Res.string.appearance_system
    }
    return UiText.StringResource(resId)
}
