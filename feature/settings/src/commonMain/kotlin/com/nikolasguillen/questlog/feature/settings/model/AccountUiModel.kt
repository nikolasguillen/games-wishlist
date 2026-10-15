package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.AuthProvider

/**
 * The signed-in account as the screen shows it.
 *
 * @property email The address the provider reported, `null` when none of the linked methods shares one. It is data
 * from the provider, never a resource, so it stays a plain string.
 * @property linkedProviders The methods linked to the account. Another platform's method appears here once Firebase has
 * linked it by email.
 */
@Immutable
internal data class AccountUiModel(
    val email: String?,
    val linkedProviders: List<AuthProvider>
)
