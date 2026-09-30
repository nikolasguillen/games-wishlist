package com.nikolasguillen.questlog.core.model

/**
 * The user's chosen appearance for the app.
 *
 * The entries carry an explicit [id] because that is what is persisted — never the enum name.
 *
 * @property id Stable identifier written to DataStore. Never renumber existing entries.
 */
enum class AppearanceMode(val id: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        /** Falls back to [SYSTEM] for an unknown [id] rather than throwing. */
        fun fromId(id: Int): AppearanceMode = entries.find { it.id == id } ?: SYSTEM
    }
}
