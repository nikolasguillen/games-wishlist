package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a company in the gaming industry.
 *
 * Only [id] and [name] are required: a company nested in a game's `involved_companies` carries nothing else, and
 * every other field is present only when a `/companies` query asks for it.
 *
 * @property id Internal IGDB unique identifier for the company.
 * @property name The official name of the company.
 * @property description A free-text description, when the catalogue has one.
 * @property startDate The founding date, in Unix seconds.
 * @property logo The company's logo, when the catalogue has one.
 * @property developed The ids of the games the company is credited as developer of.
 */
@Serializable
data class IgdbCompany(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("start_date") val startDate: Long? = null,
    val logo: IgdbImage? = null,
    val developed: List<Int>? = null
)
