package com.example.gameswishlist.feature.lists.mapper

import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.ui.mapper.toDrawableRes
import com.example.gameswishlist.core.ui.model.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class ListsUiMapperTest {

    private fun testList(gameCount: Int) = WishlistList(
        id = 1,
        name = "RPGs to Try",
        description = "Long ones, for when I have time off",
        icon = WishlistIcon.BACKLOG,
        gameCount = gameCount
    )

    @Test
    fun `toUiModel carries over id, name, description and resolves the icon`() {
        val uiModel = testList(gameCount = 8).toUiModel()

        assertEquals(1L, uiModel.id)
        assertEquals("RPGs to Try", uiModel.name)
        assertEquals("Long ones, for when I have time off", uiModel.description)
        assertEquals(WishlistIcon.BACKLOG.toDrawableRes(), uiModel.iconRes)
    }

    @Test
    fun `toUiModel shows the exact count when at or below 100 games`() {
        assertEquals(UiText.DynamicString("8"), testList(gameCount = 8).toUiModel().gameCountText)
        assertEquals(UiText.DynamicString("100"), testList(gameCount = 100).toUiModel().gameCountText)
    }

    @Test
    fun `toUiModel caps the displayed count at 99+ above 100 games`() {
        assertEquals(UiText.DynamicString("99+"), testList(gameCount = 101).toUiModel().gameCountText)
        assertEquals(UiText.DynamicString("99+"), testList(gameCount = 5000).toUiModel().gameCountText)
    }
}
