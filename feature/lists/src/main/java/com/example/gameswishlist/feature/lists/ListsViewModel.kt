package com.example.gameswishlist.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.list.CreateListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetListsUseCase
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.feature.lists.mapper.toUiModel
import com.example.gameswishlist.feature.lists.model.WishlistListUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    getListsUseCase: GetListsUseCase,
    private val createListUseCase: CreateListUseCase
) : ViewModel() {

    internal val lists: StateFlow<List<WishlistListUiModel>> = getListsUseCase()
        .map { lists -> lists.map { it.toUiModel() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createList(name: String, description: String, icon: WishlistIcon?) {
        viewModelScope.launch {
            createListUseCase(name, description, icon)
        }
    }
}
