package com.reviewanything.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reviewanything.app.data.db.AppDatabase
import com.reviewanything.app.data.model.CheckIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CheckInViewModel(private val db: AppDatabase) : ViewModel() {

    private val _checkInDates = MutableStateFlow<Set<String>>(emptySet())
    val checkInDates: StateFlow<Set<String>> = _checkInDates

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak

    private val _checkedToday = MutableStateFlow(false)
    val checkedToday: StateFlow<Boolean> = _checkedToday

    init {
        loadCheckIns()
    }

    fun loadCheckIns() {
        viewModelScope.launch {
            val all = db.checkInDao().getAllCheckIns()
            val dates = all.map { it.checkinDate }.toSortedSet()
            _checkInDates.value = dates
            _checkedToday.value = dates.contains(today())
            _streak.value = calculateStreak(dates)
        }
    }

    fun checkInToday() {
        viewModelScope.launch {
            val today = today()
            if (db.checkInDao().getCheckInByDate(today) == null) {
                db.checkInDao().insert(CheckIn(checkinDate = today))
                loadCheckIns()
            }
        }
    }

    private fun calculateStreak(dates: Set<String>): Int {
        if (dates.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var streak = 0
        var current = Calendar.getInstance()
        while (true) {
            val dateStr = sdf.format(current.time)
            if (dates.contains(dateStr)) {
                streak++
                current.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return streak
    }

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
