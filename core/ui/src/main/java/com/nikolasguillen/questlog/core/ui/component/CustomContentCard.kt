package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing

/**
 * [titleAction] renders inline with [title], vertically centered and right of it — for a single
 * icon button acting on the card's content (e.g. retry, translate). It is only shown alongside a
 * non-null [title], since it has nothing to line up with otherwise.
 */
@Composable
fun CustomContentCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            title?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    titleAction?.invoke()
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomContentCardPreview() {
    QuestLogTheme {
        CustomContentCard(
            title = "Descrizione Gioco",
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Questo è un esempio di testo molto lungo che serve a testare il comportamento del wrapping all'interno della card. Il testo dovrebbe andare a capo automaticamente rispettando il padding interno che abbiamo impostato nella Column.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomContentCardWithTitleActionPreview() {
    QuestLogTheme {
        CustomContentCard(
            title = "Descrizione Gioco",
            titleAction = {
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Testo di esempio per la variante con un'azione accanto al titolo.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
