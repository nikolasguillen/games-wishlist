package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.feature.search.model.EntityResultUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.developer_card_content_description
import org.jetbrains.compose.resources.stringResource

/** One developer in the results grid: its logo on a rounded tile with the name under it. */
@Composable
internal fun DeveloperResultCard(
    item: EntityResultUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EntityResultCard(
        name = item.name,
        contentDescription = stringResource(Res.string.developer_card_content_description, item.name),
        onClick = onClick,
        modifier = modifier
    ) { imageModifier ->
        DeveloperLogo(imageUrl = item.imageUrl, modifier = imageModifier)
    }
}

@QuestLogPreviews
@Composable
private fun DeveloperResultCardPreview() {
    QuestLogTheme {
        DeveloperResultCard(
            item = EntityResultUiModel(id = 1, name = "Capcom", imageUrl = null),
            onClick = {},
            modifier = Modifier.width(160.dp)
        )
    }
}
