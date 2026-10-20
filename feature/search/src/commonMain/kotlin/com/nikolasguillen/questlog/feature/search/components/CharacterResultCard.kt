package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.feature.search.model.EntityResultUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.character_card_content_description
import org.jetbrains.compose.resources.stringResource

/** One character in the results grid: a circular portrait with the name under it. */
@Composable
internal fun CharacterResultCard(
    item: EntityResultUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EntityResultCard(
        name = item.name,
        contentDescription = stringResource(Res.string.character_card_content_description, item.name),
        onClick = onClick,
        modifier = modifier
    ) { imageModifier ->
        CharacterPortrait(imageUrl = item.imageUrl, modifier = imageModifier)
    }
}

@QuestLogPreviews
@Composable
private fun CharacterResultCardPreview() {
    QuestLogTheme {
        CharacterResultCard(
            item = EntityResultUiModel(id = 1, name = "Kratos", imageUrl = null),
            onClick = {},
            modifier = Modifier.width(160.dp)
        )
    }
}

@QuestLogPreviews
@Composable
private fun CharacterResultCardLongNamePreview() {
    QuestLogTheme {
        CharacterResultCard(
            item = EntityResultUiModel(id = 2, name = "Commander Shepard of the Normandy Alliance Fleet", imageUrl = null),
            onClick = {},
            modifier = Modifier.width(160.dp)
        )
    }
}
