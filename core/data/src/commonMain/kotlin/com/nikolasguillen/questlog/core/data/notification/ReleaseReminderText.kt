package com.nikolasguillen.questlog.core.data.notification

/**
 * The words of a release reminder. `:shared` implements it from `:core:ui`'s Compose resources, so `:core:data`
 * needs no Compose and both platforms read one string. Public because `:shared` implements it.
 */
interface ReleaseReminderText {
    /** The title for [gameName], which is text from the data source and is inserted as is. */
    suspend fun title(gameName: String): String

    suspend fun body(): String
}
