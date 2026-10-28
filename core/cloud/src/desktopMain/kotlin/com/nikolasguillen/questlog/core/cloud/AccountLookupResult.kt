package com.nikolasguillen.questlog.core.cloud

/** What `accounts:lookup` returns about the signed-in account. [providerIds] are Firebase's names, `google.com`. */
internal class AccountLookupResult(
    val email: String?,
    val providerIds: List<String>
)
