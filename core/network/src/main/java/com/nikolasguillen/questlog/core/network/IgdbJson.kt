package com.nikolasguillen.questlog.core.network

import kotlinx.serialization.json.Json

/**
 * How IGDB's JSON is read. IGDB leaves out any field it has no value for, so an absent nullable field is
 * `null` rather than an error, and it adds fields freely, so unknown ones are ignored.
 */
internal val IgdbJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
