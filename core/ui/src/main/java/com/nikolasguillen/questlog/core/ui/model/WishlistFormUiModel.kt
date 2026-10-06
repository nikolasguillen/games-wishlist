package com.nikolasguillen.questlog.core.ui.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.WishlistIcon

/**
 * The values of the wishlist form, both what it starts with and what it hands back on confirm.
 * The default instance is the empty form used to create a list.
 *
 * @property name The list's name. On confirm it is already trimmed and non-blank.
 * @property description The list's description, already trimmed on confirm. Empty means none.
 * @property icon The chosen icon, or null for the default one.
 * @property coverImage The cover shown by the form: a stored file path when pre-filled, a picked
 *   `content://` URI after the user chooses an image, or null for no cover.
 */
@Immutable
data class WishlistFormUiModel(
    val name: String = "",
    val description: String = "",
    val icon: WishlistIcon? = null,
    val coverImage: String? = null
)
