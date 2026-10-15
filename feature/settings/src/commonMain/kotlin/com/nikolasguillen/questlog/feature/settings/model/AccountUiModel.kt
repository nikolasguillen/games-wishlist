package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.AuthProvider

/**
 * The signed-in account as the screen shows it.
 *
 * @property email The address the provider reported, `null` when none of the linked methods shares one. It is data
 * from the provider, never a resource, so it stays a plain string.
 * @property linkedProviders The methods already linked to the account.
 * @property linkableProviders The methods that can still be linked.
 */
@Immutable
internal data class AccountUiModel(
    val email: String?,
    val linkedProviders: List<AuthProvider>,
    val linkableProviders: List<AuthProvider>
)
