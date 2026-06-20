package com.example.gameswishlist.core.ui.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import androidx.compose.ui.res.stringResource

/**
 * Converts a string with HTML tags into an [AnnotatedString].
 * Useful for displaying styled text from string resources in Compose.
 */
fun String.parseHtml(): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)
    return spanned.toAnnotatedString()
}

/**
 * Extension to convert [Spanned] to [AnnotatedString], preserving basic styles like Bold, Italic, and Underline.
 */
fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    val spanned = this@toAnnotatedString
    append(spanned.toString())
    getSpans(0, length, Any::class.java).forEach { span ->
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        when (span) {
            is StyleSpan -> when (span.style) {
                Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                Typeface.BOLD_ITALIC -> addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    start,
                    end
                )
            }
            is UnderlineSpan -> addStyle(
                SpanStyle(textDecoration = TextDecoration.Underline),
                start,
                end
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
