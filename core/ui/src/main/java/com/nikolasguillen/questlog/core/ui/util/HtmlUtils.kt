package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

private val TAG = Regex("<(/?)([biu])>", RegexOption.IGNORE_CASE)

private fun styleOf(tag: String): SpanStyle = when (tag.lowercase()) {
    "b" -> SpanStyle(fontWeight = FontWeight.Bold)
    "i" -> SpanStyle(fontStyle = FontStyle.Italic)
    else -> SpanStyle(textDecoration = TextDecoration.Underline)
}

/**
 * Converts a string with `<b>`, `<i>` and `<u>` tags into an [AnnotatedString]. Useful for displaying
 * styled text from string resources in Compose.
 *
 * Tags may nest. Anything that is not a matched pair of those three tags - another tag, an unclosed or an
 * unopened one, a stray `<` - is kept as literal text, so a string is never silently shortened.
 */
fun String.parseHtml(): AnnotatedString {
    val source = this

    // Pair every opening tag with the closing tag that ends it. A closing tag with no opener is left alone,
    // and an opener that gets skipped by a mismatched closer is dropped from the stack, so it stays literal.
    val open = ArrayDeque<MatchResult>()
    val pairs = mutableListOf<Pair<MatchResult, MatchResult>>()
    for (match in TAG.findAll(source)) {
        val name = match.groupValues[2].lowercase()
        if (match.groupValues[1].isEmpty()) {
            open.addLast(match)
        } else {
            val index = open.indexOfLast { it.groupValues[2].lowercase() == name }
            if (index >= 0) {
                pairs += open[index] to match
                while (open.size > index) open.removeLast()
            }
        }
    }

    // The tags of a pair are removed from the text; spans are then re-anchored to the shortened text.
    val removed = pairs.flatMap { listOf(it.first.range, it.second.range) }.sortedBy { it.first }
    fun shortened(sourceIndex: Int): Int = sourceIndex - removed.filter { it.last < sourceIndex }.sumOf { it.count() }

    val text = buildString {
        source.forEachIndexed { index, char -> if (removed.none { index in it }) append(char) }
    }
    return buildAnnotatedString {
        append(text)
        pairs.forEach { (opening, closing) ->
            addStyle(
                styleOf(opening.groupValues[2]),
                shortened(opening.range.last + 1),
                shortened(closing.range.first)
            )
        }
    }
}

/**
 * Composable function to get a formatted and styled string resource.
 */
@Composable
@ReadOnlyComposable
fun annotatedStringResource(id: Int, vararg args: Any): AnnotatedString {
    val rawString = stringResource(id, *args)
    return rawString.parseHtml()
}
