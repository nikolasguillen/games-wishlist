package com.example.gameswishlist.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.mapper.toUiModel
import com.example.gameswishlist.core.ui.R as CoreUiR

@Immutable
internal data class GameDetailUiModel(
    val id: Int,
    val name: UiText,
    val description: UiText,
    val images: List<String>,
    val gameType: UiText,
    val rating: RatingUiModel?,
    val availability: AvailabilityUiModel,
    val genres: List<UiText>,
    val companyInfo: UiText,
    val isWishlisted: Boolean,
    val personalDetails: GameDetailPersonalUiModel,
    val relatedGames: List<RelatedGamesUiModel>
) {
    companion object {
        /** Sample data for `@Preview`s of the detail screen and of its components. */
        fun getDummy() = GameDetailUiModel(
            id = 1,
            name = UiText.DynamicString("The Witcher 3: Wild Hunt"),
            description = UiText.DynamicString("A legendary RPG with a rich story and vast open world."),
            images = emptyList(),
            gameType = UiText.DynamicString("Main Game"),
            rating = RatingUiModel(
                score = 95,
                scoreText = UiText.DynamicString("95"),
                scoreLabel = UiText.DynamicString("Metascore"),
                hypes = UiText.DynamicString("120"),
                hypesLabel = UiText.StringResource(CoreUiR.string.hypes_title),
                ratingCount = UiText.DynamicString("450"),
                ratingCountLabel = UiText.StringResource(CoreUiR.string.rating_count_title)
            ),
            availability = AvailabilityUiModel(
                mainDate = UiText.DynamicString("May 19, 2015"),
                platforms = listOf(
                    PlatformTileUiModel(id = 1, code = UiText.DynamicString("PC"), color = Color(0xFF5E5E5E)),
                    PlatformTileUiModel(id = 2, code = UiText.DynamicString("PS4"), color = Color(0xFF2E4EA6)),
                    PlatformTileUiModel(id = 3, code = UiText.DynamicString("XB1"), color = Color(0xFF107C10)),
                    PlatformTileUiModel(id = 4, code = UiText.DynamicString("SWI"), color = Color(0xFFE60012))
                ),
                detailedDates = emptyList(),
                isExpandable = false
            ),
            genres = listOf("RPG", "Action").map { UiText.DynamicString(it) },
            companyInfo = UiText.DynamicString("CD Projekt Red, CD Projekt"),
            isWishlisted = false,
            personalDetails = GameDetailPersonalUiModel(
                notes = UiText.DynamicString("Geralt's adventures are amazing!"),
                availableStatuses = GameStatus.entries.mapIndexed { index, status ->
                    status.toUiModel(index == 1)
                },
                availablePriorities = Priority.entries.mapIndexed { index, priority ->
                    priority.toUiModel(index == 1)
                }
            ),
            relatedGames = emptyList()
        )
    }
}
