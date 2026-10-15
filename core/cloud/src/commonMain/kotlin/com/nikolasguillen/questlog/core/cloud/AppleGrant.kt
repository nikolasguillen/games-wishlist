package com.nikolasguillen.questlog.core.cloud

/** Opaque to common code: an authorization code (iOS) or an OAuth access token (Android). */
class AppleGrant internal constructor(internal val value: String)
