package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.model.Developer
import com.nikolasguillen.questlog.core.model.DeveloperProfile
import com.nikolasguillen.questlog.core.network.model.IgdbCompany
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val LOGO_IMAGE_SIZE = "t_logo_med"

internal fun IgdbCompany.toDeveloper(): Developer = Developer(
    id = id,
    name = name,
    logoUrl = logo?.url?.toIgdbImageUrl(LOGO_IMAGE_SIZE),
    developedGameCount = developed.orEmpty().size
)

internal fun IgdbCompany.toDeveloperProfile(): DeveloperProfile = DeveloperProfile(
    id = id,
    name = name,
    logoUrl = logo?.url?.toIgdbImageUrl(LOGO_IMAGE_SIZE),
    description = description?.takeIf { it.isNotBlank() },
    foundedYear = startDate?.let { Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.UTC).year }
)
