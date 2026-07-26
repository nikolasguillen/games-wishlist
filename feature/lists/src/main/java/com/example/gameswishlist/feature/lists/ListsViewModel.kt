package com.example.gameswishlist.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.list.CreateListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetListsUseCase
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.feature.lists.mapper.toUiModel
import com.example.gameswishlist.feature.lists.model.WishlistListUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    getListsUseCase: GetListsUseCase,
    private val createListUseCase: CreateListUseCase
) : ViewModel() {

    private val _lists = MutableStateFlow<List<WishlistListUiModel>>(emptyList())

    internal val lists: StateFlow<List<WishlistListUiModel>> = _lists.asStateFlow()

    init {
        viewModelScope.launch {
            getListsUseCase().collect { lists ->
                _lists.value = lists.map { it.toUiModel() }
            }
        }
    }

    init {
        viewModelScope.launch {
            getListsUseCase().collect { lists ->
                _lists.value = lists.map { it.toUiModel() }
            }
        }
    }

    fun createList(name: String, description: String, icon: WishlistIcon?) {
        viewModelScope.launch {
            createListUseCase(name, description, icon)
        }
    }
}
