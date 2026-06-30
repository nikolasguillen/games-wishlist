package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing

@Composable
fun CustomContentCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomContentCardPreview() {
    GamesWishlistTheme {
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
