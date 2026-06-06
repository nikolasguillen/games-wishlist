package com.example.gameswishlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.navigation.*
import com.example.gameswishlist.feature.search.*
import com.example.gameswishlist.feature.gamedetail.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GamesWishlistTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent() {
    val backStack = rememberNavBackStack(SearchRoute as NavKey)
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = backStack.last() is SearchRoute,
                    onClick = { 
                        if (backStack.last() !is SearchRoute) {
                            backStack.add(SearchRoute)
                        }
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = backStack.last() is ListsRoute || backStack.last() is WishlistRoute,
                    onClick = { 
                        if (backStack.last() !is ListsRoute) {
                            backStack.add(ListsRoute)
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lists") },
                    label = { Text("Lists") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { 
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.size - 1)
                }
            },
            modifier = Modifier.padding(innerPadding),
            entryProvider = { key ->
                when (key) {
                    is SearchRoute -> NavEntry(key) {
                        val vm: SearchViewModel = viewModel()
                        SearchScreen(
                            viewModel = vm,
                            onGameClick = { gameId: Int ->
                                backStack.add(GameDetailRoute(gameId))
                            }
                        )
                    }
                    is ListsRoute -> NavEntry(key) {
                        val vm: ListsViewModel = viewModel()
                        ListsScreen(
                            viewModel = vm,
                            onListClick = { listId: Long, listName: String ->
                                backStack.add(WishlistRoute(listId, listName))
                            }
                        )
                    }
                    is WishlistRoute -> NavEntry(key) {
                        val vm: WishlistViewModel = viewModel()
                        WishlistScreen(
                            listId = key.listId,
                            listName = key.listName,
                            viewModel = vm,
                            onGameClick = { gameId: Int ->
                                backStack.add(GameDetailRoute(gameId))
                            },
                            onBackClick = {
                                backStack.removeAt(backStack.size - 1)
                            }
                        )
                    }
                    is GameDetailRoute -> NavEntry(key) {
                        val vm: GameDetailViewModel = viewModel()
                        GameDetailScreen(
                            gameId = key.gameId,
                            viewModel = vm,
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeAt(backStack.size - 1)
                                }
                            }
                        )
                    }
                    else -> NavEntry(key) { }
                }
            }
        )
    }
}
