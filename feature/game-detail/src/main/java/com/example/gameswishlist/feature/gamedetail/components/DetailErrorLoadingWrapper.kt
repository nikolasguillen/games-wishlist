package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.core.ui.R as CoreUiR

/**
 * A simple wrapper for Error and Loading states in the Game Detail feature to provide a back button.
 * Marked as internal to be used only within the feature module.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailErrorLoadingWrapper(
    onBackClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiR.string.back_content_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailErrorLoadingWrapperPreview() {
    GamesWishlistTheme {
        DetailErrorLoadingWrapper(onBackClick = {}) {
            LoadingPage()
        }
    }
}
