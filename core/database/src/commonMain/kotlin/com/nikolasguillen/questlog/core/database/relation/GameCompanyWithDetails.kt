package com.nikolasguillen.questlog.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.nikolasguillen.questlog.core.database.entity.CompanyEntity
import com.nikolasguillen.questlog.core.database.entity.GameCompanyCrossRef

data class GameCompanyWithDetails(
    @Embedded val crossRef: GameCompanyCrossRef,
    @Relation(
        parentColumn = "companyId",
        entityColumn = "id"
    )
    val company: CompanyEntity
)
