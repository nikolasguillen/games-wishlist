package com.example.gameswishlist.core.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A wrapper class to handle strings that can be either plain text or Android string resources.
 * This allows the ViewModel to remain agnostic of the Android Context while still providing
 * localized strings to the UI.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }
}
