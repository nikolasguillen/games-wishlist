package com.nikolasguillen.questlog.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.nikolasguillen.questlog.core.database.entity.CachedGameCompanyCrossRef
import com.nikolasguillen.questlog.core.database.entity.CompanyEntity

data class CachedGameCompanyWithDetails(
    @Embedded val crossRef: CachedGameCompanyCrossRef,
    @Relation(
        parentColumn = "companyId",
        entityColumn = "id"
    )
    val company: CompanyEntity
)
