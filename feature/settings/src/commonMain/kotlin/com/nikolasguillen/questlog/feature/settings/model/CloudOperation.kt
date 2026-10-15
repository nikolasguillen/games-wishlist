package com.nikolasguillen.questlog.feature.settings.model

/** A cloud action that can be running. The screen runs one at a time. */
internal enum class CloudOperation {
    SIGN_IN,
    BACK_UP,
    RESTORE,
    SIGN_OUT,
    DELETE_ACCOUNT
}
