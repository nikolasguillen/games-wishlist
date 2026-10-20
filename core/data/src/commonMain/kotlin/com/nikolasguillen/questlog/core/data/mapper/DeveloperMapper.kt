package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.model.Developer
import com.nikolasguillen.questlog.core.model.DeveloperProfile
import com.nikolasguillen.questlog.core.network.model.IgdbCompany
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val LOGO_IMAGE_SIZE = "t_logo_med"

/**
 * A logo as a PNG. IGDB serves the `.jpg` address its API returns as a JPEG, which has no transparency: a logo drawn
 * on a transparent background comes back on solid black, and black artwork disappears into it.
 */
private fun String.toLogoUrl(): String = toIgdbImageUrl(LOGO_IMAGE_SIZE).replace(".jpg", ".png")

internal fun IgdbCompany.toDeveloper(): Developer = Developer(
    id = id,
    name = name,
    logoUrl = logo?.url?.toLogoUrl(),
    developedGameCount = developed.orEmpty().size
)

internal fun IgdbCompany.toDeveloperProfile(): DeveloperProfile = DeveloperProfile(
    id = id,
    name = name,
    logoUrl = logo?.url?.toLogoUrl(),
    description = description?.takeIf { it.isNotBlank() },
    foundedYear = startDate?.let { Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.UTC).year }
)
