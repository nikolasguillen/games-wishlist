package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.database.entity.SearchHistoryEntity
import com.example.gameswishlist.core.model.SearchHistoryItem

fun List<SearchHistoryEntity>.toSearchHistoryItems(): List<SearchHistoryItem> {
    return map { it.toSearchHistoryItem() }
}

fun SearchHistoryEntity.toSearchHistoryItem(): SearchHistoryItem {
    return SearchHistoryItem(
        query = query,
        timestamp = timestamp
    )
}