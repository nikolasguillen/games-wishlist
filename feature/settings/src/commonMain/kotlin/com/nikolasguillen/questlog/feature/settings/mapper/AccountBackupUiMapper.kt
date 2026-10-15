package com.nikolasguillen.questlog.feature.settings.mapper

import com.nikolasguillen.questlog.core.common.DateUtils
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AuthProvider
import com.nikolasguillen.questlog.core.model.BackupInfo
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.settings.model.AccountUiModel
import com.nikolasguillen.questlog.feature.settings.model.BackupStatusUiModel
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_last_backup
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_provider_apple
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_provider_google
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_summary_games
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_summary_lists

private const val SUMMARY_SEPARATOR = ", "

internal fun Account.toUiModel() = AccountUiModel(
    email = email,
    linkedProviders = AuthProvider.entries.filter { it in providers }
)

/**
 * The backup card's content. The date is formatted here, in the device's locale and time zone, so no composable has to.
 * The counts are plurals, so "1 list, 1 game" reads correctly.
 */
internal fun BackupInfo.toUiModel() = BackupStatusUiModel.Available(
    lastBackup = UiText.StringResource(Res.string.account_backup_last_backup, DateUtils.formatDateTime(createdAt)),
    summary = UiText.CompoundString(
        texts = listOf(
            UiText.PluralResource(Res.plurals.account_backup_summary_lists, listCount, listCount),
            UiText.PluralResource(Res.plurals.account_backup_summary_games, gameCount, gameCount)
        ),
        separator = SUMMARY_SEPARATOR
    )
)

internal fun AuthProvider.toLabelUiText(): UiText = when (this) {
    AuthProvider.GOOGLE -> UiText.StringResource(Res.string.account_backup_provider_google)
    AuthProvider.APPLE -> UiText.StringResource(Res.string.account_backup_provider_apple)
}
