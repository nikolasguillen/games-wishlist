package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.common.capitalize
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.ui.R

@Composable
fun GameDetailPersonalCard(
    status: GameStatus,
    priority: Priority,
    notes: String,
    onStatusChange: (GameStatus) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.personal_progress_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Status
            Text(stringResource(R.string.status_label), style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameStatus.entries.forEach { entry ->
                    FilterChip(
                        selected = status == entry,
                        onClick = { onStatusChange(entry) },
                        label = {
                            Text(
                                entry.name
                                    .lowercase()
                                    .replace("_", " ")
                                    .capitalize()
                            )
                        }
                    )
                }
            }

            // Priority
            Text(
                stringResource(R.string.priority_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = priority.ordinal.toFloat(),
                onValueChange = { onPriorityChange(Priority.entries[it.toInt()]) },
                valueRange = 0f..2f,
                steps = 1
            )
            Text(
                text = stringResource(
                    when (priority) {
                        Priority.LOW -> R.string.priority_low
                        Priority.MEDIUM -> R.string.priority_medium
                        Priority.HIGH -> R.string.priority_high
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )

            // Notes
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.personal_notes_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameDetailPersonalCardPreview() {
    MaterialTheme {
        GameDetailPersonalCard(
            status = GameStatus.PLAYING,
            priority = Priority.MEDIUM,
            notes = "Loving the open world so far!",
            onStatusChange = {},
            onPriorityChange = {},
            onNotesChange = {}
        )
    }
}
