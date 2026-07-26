package com.example.gameswishlist.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.feature.lists.components.CreateWishlistCard
import com.example.gameswishlist.feature.lists.components.CreateWishlistSheet
import com.example.gameswishlist.feature.lists.components.WishlistRow
import com.example.gameswishlist.feature.lists.model.ListsUiEffect

@Composable
fun ListsScreen(
    viewModel: ListsViewModel,
    onListClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    var showCreateSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is ListsUiEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.wishlist_hub_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumLarge)
        ) {
            item {
                CreateWishlistCard(onClick = { showCreateSheet = true })
            }
            items(lists, key = { it.id }) { list ->
                WishlistRow(
                    list = list,
                    onClick = { onListClick(list.id, list.name) }
                )
            }
        }

        if (showCreateSheet) {
            CreateWishlistSheet(
                onDismiss = { showCreateSheet = false },
                onCreate = { name, description, icon, coverImageUri ->
                    viewModel.createList(name, description, icon, coverImageUri)
                    showCreateSheet = false
                }
            )
        }
    }
}
