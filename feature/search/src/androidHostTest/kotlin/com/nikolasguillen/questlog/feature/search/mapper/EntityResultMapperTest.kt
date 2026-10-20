package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.model.Developer
import com.nikolasguillen.questlog.core.model.GameCharacter
import com.nikolasguillen.questlog.feature.search.model.EntityResultUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers the two mappers that turn a character or a developer into the card of a results grid. */
class EntityResultMapperTest {

    @Test
    fun `a character keeps its id, name and portrait`() {
        val character = GameCharacter(id = 7, name = "Kratos", imageUrl = "https://img/kratos.jpg")

        assertEquals(
            EntityResultUiModel(id = 7, name = "Kratos", imageUrl = "https://img/kratos.jpg"),
            character.toEntityResultUiModel()
        )
    }

    @Test
    fun `a character without a portrait has no image`() {
        assertNull(GameCharacter(id = 7, name = "Young Kratos", imageUrl = null).toEntityResultUiModel().imageUrl)
    }

    @Test
    fun `a developer shows its logo as the image`() {
        val developer = Developer(id = 9, name = "Capcom", logoUrl = "https://img/capcom.png", developedGameCount = 500)

        assertEquals(
            EntityResultUiModel(id = 9, name = "Capcom", imageUrl = "https://img/capcom.png"),
            developer.toEntityResultUiModel()
        )
    }

    @Test
    fun `a developer without a logo has no image`() {
        assertNull(Developer(id = 9, name = "Capcom", logoUrl = null, developedGameCount = 1).toEntityResultUiModel().imageUrl)
    }
}
