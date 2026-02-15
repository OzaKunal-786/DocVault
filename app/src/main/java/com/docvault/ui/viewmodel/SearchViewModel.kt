package com.docvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docvault.data.database.DocumentEntity
import com.docvault.data.repository.DocumentRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    init {
        // Debounce search to avoid querying on every keystroke
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) {
                    search(query)
                } else {
                    _searchResults.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun search(query: String) {
        viewModelScope.launch {
            // Using FTS4 search from repository
            repository.searchDocuments(query).collect { results ->
                _searchResults.value = results
            }
        }
    }
}
