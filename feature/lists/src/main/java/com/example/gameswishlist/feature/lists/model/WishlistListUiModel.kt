package com.example.gameswishlist.feature.lists.model

import androidx.annotation.DrawableRes
import com.example.gameswishlist.core.ui.model.UiText
import java.io.File

internal data class WishlistListUiModel(
    val id: Long,
    val name: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    val coverImagePath: String?,
    val gameCountText: UiText
) {
    val coverImageFile: File? get() = coverImagePath?.let { File(it) }
}