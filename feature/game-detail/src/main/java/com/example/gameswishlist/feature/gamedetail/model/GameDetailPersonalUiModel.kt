package com.example.gameswishlist.feature.gamedetail.model

data class GameDetailPersonalUiModel(
    val notes: String,
    val availableStatuses: List<GameStatusUiModel>,
    val availablePriorities: List<PriorityUiModel>
)