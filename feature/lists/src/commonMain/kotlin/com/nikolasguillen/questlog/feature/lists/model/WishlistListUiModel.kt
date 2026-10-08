package com.nikolasguillen.questlog.feature.lists.model

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal data class WishlistListUiModel(
    val id: Long,
    val name: String,
    val description: String,
    val iconRes: DrawableResource,
    val coverImagePath: String?,
    val gameCountText: UiText,
    val isDefault: Boolean
)
