package com.reviewanything.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reviewanything.app.data.model.ReviewItem
import com.reviewanything.app.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(private val repository: ReviewRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<ReviewItem>>(emptyList())
    val items: StateFlow<List<ReviewItem>> = _items

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished

    private val _empty = MutableStateFlow(false)
    val empty: StateFlow<Boolean> = _empty

    private val _stats = MutableStateFlow(ReviewStats())
    val stats: StateFlow<ReviewStats> = _stats

    private val _dailyCount = MutableStateFlow(10)
    val dailyCount: StateFlow<Int> = _dailyCount

    fun setDailyCount(count: Int) {
        _dailyCount.value = count.coerceIn(1, 100)
    }

    fun loadItems(count: Int) {
        viewModelScope.launch {
            val due = repository.getDueItems(count)
            _items.value = due
            _currentIndex.value = 0
            _finished.value = false
            _empty.value = due.isEmpty()
            _stats.value = ReviewStats(total = due.size, forget = 0)
        }
    }

    fun onRemembered() {
        val item = _items.value.getOrNull(_currentIndex.value) ?: return
        viewModelScope.launch {
            repository.markRemembered(item)
            next()
        }
    }

    fun onForgotten() {
        val item = _items.value.getOrNull(_currentIndex.value) ?: return
        viewModelScope.launch {
            repository.markForgotten(item)
            _stats.value = _stats.value.copy(forget = _stats.value.forget + 1)
            next()
        }
    }

    private fun next() {
        if (_currentIndex.value + 1 >= _items.value.size) {
            _finished.value = true
        } else {
            _currentIndex.value += 1
        }
    }

    fun restart() {
        _items.value = emptyList()
        _currentIndex.value = 0
        _finished.value = false
        _empty.value = false
        _stats.value = ReviewStats()
    }

    data class ReviewStats(val total: Int = 0, val forget: Int = 0)
}
