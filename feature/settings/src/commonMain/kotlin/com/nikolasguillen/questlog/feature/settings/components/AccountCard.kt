package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.AuthProvider
import com.nikolasguillen.questlog.core.ui.component.CustomContentCard
import com.nikolasguillen.questlog.feature.settings.mapper.toLabelUiText
import com.nikolasguillen.questlog.feature.settings.model.AccountUiModel
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_linked_methods
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_signed_in_as
import org.jetbrains.compose.resources.stringResource

/** Who is signed in and with which methods. */
@Composable
internal fun AccountCard(account: AccountUiModel, modifier: Modifier = Modifier) {
    CustomContentCard(modifier = modifier.fillMaxWidth()) {
        Column {
            account.email?.let { email ->
                Text(
                    text = stringResource(Res.string.account_backup_signed_in_as),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Row {
                Text(
                    text = stringResource(Res.string.account_backup_linked_methods),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = account.linkedProviders.map { it.toLabelUiText().asString() }
                        .joinToString(separator = ", "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun AccountCardPreview() {
    QuestLogTheme {
        AccountCard(
            account = AccountUiModel(
                email = "ada@example.com",
                linkedProviders = listOf(AuthProvider.GOOGLE)
            )
        )
    }
}
