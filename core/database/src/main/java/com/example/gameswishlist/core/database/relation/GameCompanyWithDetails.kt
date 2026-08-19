package com.example.gameswishlist.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gameswishlist.core.database.entity.CompanyEntity
import com.example.gameswishlist.core.database.entity.GameCompanyCrossRef

data class GameCompanyWithDetails(
    @Embedded val crossRef: GameCompanyCrossRef,
    @Relation(
        parentColumn = "companyId",
        entityColumn = "id"
    )
    val company: CompanyEntity
)
