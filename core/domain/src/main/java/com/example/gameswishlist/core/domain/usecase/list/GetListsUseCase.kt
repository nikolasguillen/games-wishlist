package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.WishlistList
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetListsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<List<WishlistList>> {
        return repository.getAllLists()
    }
}
