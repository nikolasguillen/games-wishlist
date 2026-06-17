package com.example.gameswishlist.feature.gamedetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gameswishlist.core.common.capitalize
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: Int,
    viewModel: GameDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableLists by viewModel.availableLists.collectAsStateWithLifecycle()
    var showListSelector by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        viewModel.loadGame(gameId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.game?.name ?: "Game Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showListSelector = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Add to List")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            uiState.game?.let { game ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = game.backgroundImage,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = game.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Personal Metadata Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Personal Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Status
                                Text("Status:", style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GameStatus.entries.forEach { status ->
                                        FilterChip(
                                            selected = game.status == status,
                                            onClick = { viewModel.updateStatus(status) },
                                            label = {
                                                Text(
                                                    status.name
                                                        .lowercase()
                                                        .replace("_", " ")
                                                        .capitalize()
                                                )
                                            }
                                        )
                                    }
                                }

                                // Priority
                                Text("Priority:", style = MaterialTheme.typography.bodyMedium)
                                Slider(
                                    value = game.priority.ordinal.toFloat(),
                                    onValueChange = { viewModel.updatePriority(Priority.entries[it.toInt()]) },
                                    valueRange = 0f..2f,
                                    steps = 1
                                )
                                Text(
                                    when (game.priority) {
                                        Priority.LOW -> "Low"
                                        Priority.MEDIUM -> "Medium"
                                        Priority.HIGH -> "High"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )

                                // Notes
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = game.notes,
                                    onValueChange = { viewModel.updateNotes(it) },
                                    label = { Text("Personal Notes") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Rating: ${game.rating}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            game.metaCritic?.let {
                                Text(
                                    text = "Metacritic: $it",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = game.description, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Platforms",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = game.platforms.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Genres",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = game.genres.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (showListSelector) {
            ListSelectorDialog(
                lists = availableLists,
                onDismiss = { showListSelector = false },
                onListSelected = { listId ->
                    viewModel.addGameToList(listId)
                    showListSelector = false
                }
            )
        }
    }
}

@Composable
fun ListSelectorDialog(
    lists: List<com.example.gameswishlist.core.model.WishlistList>,
    onDismiss: () -> Unit,
    onListSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select List") },
        text = {
            if (lists.isEmpty()) {
                Text("No lists found. Go to 'My Lists' to create one.")
            } else {
                LazyColumn {
                    items(lists) { list ->
                        ListItem(
                            headlineContent = { Text(list.name) },
                            modifier = Modifier.clickable { onListSelected(list.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
