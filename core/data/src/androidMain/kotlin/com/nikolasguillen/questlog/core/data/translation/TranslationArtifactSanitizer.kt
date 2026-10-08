package com.nikolasguillen.questlog.core.data.translation

/**
 * A small on-device model will occasionally ignore the output-format instruction regardless of how the
 * prompt is worded, most often by echoing a `Description:`/`Descrizione:`-style label or wrapping the
 * answer in a code fence or the `<text>` tag from the prompt itself.
 */
internal fun String.stripTranslationArtifacts(): String {
    var text = trim()

    if (text.startsWith("```") && text.endsWith("```")) {
        text = text.removePrefix("```").removeSuffix("```")
        text = text.substringAfter("\n", text).trim()
    }

    text = text.removePrefix("<text>").removeSuffix("</text>").trim()

    text = text.replaceFirst(Regex("^\\p{L}{1,20}:\\s*"), "")

    if (text.length >= 2 && text.first() == '"' && text.last() == '"' && text.count { it == '"' } == 2) {
        text = text.substring(1, text.length - 1)
    }

    return text.trim()
}
