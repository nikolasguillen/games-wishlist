package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.error_account_exists_other_method
import com.nikolasguillen.questlog.core.ui.resources.error_backup_newer_version
import com.nikolasguillen.questlog.core.ui.resources.error_backup_not_found
import com.nikolasguillen.questlog.core.ui.resources.error_backup_too_large
import com.nikolasguillen.questlog.core.ui.resources.error_backup_unreadable
import com.nikolasguillen.questlog.core.ui.resources.error_file_storage
import com.nikolasguillen.questlog.core.ui.resources.error_http
import com.nikolasguillen.questlog.core.ui.resources.error_method_linked_other_account
import com.nikolasguillen.questlog.core.ui.resources.error_no_network
import com.nikolasguillen.questlog.core.ui.resources.error_not_signed_in
import com.nikolasguillen.questlog.core.ui.resources.error_request_timeout
import com.nikolasguillen.questlog.core.ui.resources.error_unknown

fun RepositoryError.toUiText(): UiText {
    return when (this) {
        RepositoryError.NoNetwork -> UiText.StringResource(Res.string.error_no_network)
        RepositoryError.RequestTimeout -> UiText.StringResource(Res.string.error_request_timeout)
        is RepositoryError.Http -> UiText.StringResource(Res.string.error_http, code)
        RepositoryError.FileStorage -> UiText.StringResource(Res.string.error_file_storage)
        RepositoryError.NotSignedIn -> UiText.StringResource(Res.string.error_not_signed_in)
        // Never shown: the screen ignores a cancelled sign-in. The branch exists only to keep the `when` exhaustive.
        RepositoryError.SignInCancelled -> UiText.StringResource(Res.string.error_unknown)
        RepositoryError.AccountExistsWithOtherMethod ->
            UiText.StringResource(Res.string.error_account_exists_other_method)
        RepositoryError.MethodLinkedToOtherAccount ->
            UiText.StringResource(Res.string.error_method_linked_other_account)
        RepositoryError.BackupNotFound -> UiText.StringResource(Res.string.error_backup_not_found)
        RepositoryError.BackupFromNewerVersion -> UiText.StringResource(Res.string.error_backup_newer_version)
        RepositoryError.BackupUnreadable -> UiText.StringResource(Res.string.error_backup_unreadable)
        RepositoryError.BackupTooLarge -> UiText.StringResource(Res.string.error_backup_too_large)
        is RepositoryError.Unknown -> UiText.StringResource(Res.string.error_unknown)
    }
}
