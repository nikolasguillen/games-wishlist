package com.nikolasguillen.questlog.core.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource as ResourceString

/**
 * A wrapper class to handle strings that can be either plain text or string resources.
 * This allows the ViewModel to remain agnostic of the platform while still providing
 * localized strings to the UI.
 * Also provides two functions to convert the string to a String:
 * - asString() in a composable context
 * - suspend resolve() everywhere else, such as an effect that shows a snackbar
 */
@Immutable
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    class StringResource(
        val res: ResourceString,
        vararg args: Any
    ) : UiText() {
        val args: List<Any> = args.toList()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResource) return false
            return res == other.res && args == other.args
        }

        override fun hashCode(): Int = 31 * res.hashCode() + args.hashCode()

        override fun toString(): String = "StringResource(res=${res.key}, args=$args)"
    }

    /**
     * A quantity string. [quantity] only selects the plural form; pass it again in [args] if the string
     * prints it (`%1$d games`).
     */
    class PluralResource(
        val res: PluralStringResource,
        val quantity: Int,
        vararg args: Any
    ) : UiText() {
        val args: List<Any> = args.toList()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PluralResource) return false
            return res == other.res && quantity == other.quantity && args == other.args
        }

        override fun hashCode(): Int = 31 * (31 * res.hashCode() + quantity) + args.hashCode()

        override fun toString(): String = "PluralResource(res=${res.key}, quantity=$quantity, args=$args)"
    }

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
                stringResource(res, *resolvedArgs)
            }
            is PluralResource -> {
                val resolvedArgs = args.map { arg ->
                    if (arg is UiText) arg.asString() else arg
                }.toTypedArray()
                pluralStringResource(res, quantity, *resolvedArgs)
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

    suspend fun resolve(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                val resolvedArgs = args.map { arg ->
                    if (arg is UiText) arg.resolve() else arg
                }.toTypedArray()
                getString(res, *resolvedArgs)
            }
            is PluralResource -> {
                val resolvedArgs = args.map { arg ->
                    if (arg is UiText) arg.resolve() else arg
                }.toTypedArray()
                getPluralString(res, quantity, *resolvedArgs)
            }
            is CompoundString -> {
                buildString {
                    texts.forEachIndexed { index, uiText ->
                        append(uiText.resolve())
                        if (index < texts.lastIndex) append(separator)
                    }
                }
            }
        }
    }
}
