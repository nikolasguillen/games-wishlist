package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.Serializable

/**
 * Represents a company in the gaming industry.
 *
 * @property id Internal IGDB unique identifier for the company.
 * @property name The official name of the company.
 */
@Serializable
data class IgdbCompany(
    val id: Int,
    val name: String
)
