package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.model.Developer
import com.nikolasguillen.questlog.core.model.GameCharacter
import com.nikolasguillen.questlog.feature.search.model.EntityResultUiModel

internal fun GameCharacter.toEntityResultUiModel(): EntityResultUiModel =
    EntityResultUiModel(id = id, name = name, imageUrl = imageUrl)

internal fun Developer.toEntityResultUiModel(): EntityResultUiModel =
    EntityResultUiModel(id = id, name = name, imageUrl = logoUrl)
