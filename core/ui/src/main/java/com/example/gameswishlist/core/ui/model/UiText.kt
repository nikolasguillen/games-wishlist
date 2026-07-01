package com.example.gameswishlist.core.ui.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A wrapper class to handle strings that can be either plain text or Android string resources.
 * This allows the ViewModel to remain agnostic of the Android Context while still providing
 * localized strings to the UI.
 * Also provides two functions to convert the string to a String:
 * - asString(): String -> Returns the string as a plain String in a composable context
 * - asString(context: Context): String -> Returns the string as a String from the given Context
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    data class CompoundString(
        val texts: List<UiText>,
        val separator: String = ""
    ) : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                val resolvedArgs = args.map { arg ->
                    if (arg is UiText) arg.asString() else arg
                }.toTypedArray()
                stringResource(resId, *resolvedArgs)
            }
            is CompoundString -> {
                val stringBuilder = StringBuilder()
                texts.forEachIndexed { index, uiText ->
                    stringBuilder.append(uiText.asString())
                    if (index < texts.lastIndex) {
                        stringBuilder.append(separator)
                    }
                }
                stringBuilder.toString()
            }
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                val resolvedArgs = args.map { arg ->
                    if (arg is UiText) arg.asString(context) else arg
                }.toTypedArray()
                context.getString(resId, *resolvedArgs)
            }
            is CompoundString -> {
                texts.joinToString(separator) { it.asString(context) }
            }
        }
    }
}
