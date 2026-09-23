package com.nikolasguillen.questlog.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.list.CreateListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetListsUseCase
import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.ui.mapper.toUiText
import com.nikolasguillen.questlog.feature.lists.mapper.toUiModel
import com.nikolasguillen.questlog.feature.lists.model.ListsContentState
import com.nikolasguillen.questlog.feature.lists.model.ListsUiEffect
import com.nikolasguillen.questlog.feature.lists.model.ListsUiEvent
import com.nikolasguillen.questlog.feature.lists.model.ListsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    getListsUseCase: GetListsUseCase,
    private val createListUseCase: CreateListUseCase
) : ViewModel() {

    internal val uiState: StateFlow<ListsUiState> = getListsUseCase()
        .map { lists ->
            ListsUiState(
                contentState = ListsContentState.Success(lists.map { it.toUiModel() })
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListsUiState()
        )

    private val _uiEffect = Channel<ListsUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    internal fun onEvent(event: ListsUiEvent) {
        when (event) {
            is ListsUiEvent.OnListCreated -> createList(
                name = event.name,
                description = event.description,
                icon = event.icon,
                coverImageUri = event.coverImageUri
            )
        }
    }

    private fun createList(
        name: String,
        description: String,
        icon: WishlistIcon?,
        coverImageUri: String?
    ) {
        viewModelScope.launch {
            createListUseCase(name, description, icon, coverImageUri).onFailure { error ->
                _uiEffect.trySend(ListsUiEffect.ShowSnackbar(error.toUiText()))
            }
        }
    }
}
