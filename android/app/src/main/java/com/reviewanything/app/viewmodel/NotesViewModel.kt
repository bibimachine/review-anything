package com.reviewanything.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reviewanything.app.data.db.AppDatabase
import com.reviewanything.app.data.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesViewModel(private val db: AppDatabase) : ViewModel() {

    private val _sections = MutableStateFlow<List<String>>(emptyList())
    val sections: StateFlow<List<String>> = _sections

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _selectedSection = MutableStateFlow<String?>(null)

    init {
        loadSections()
    }

    fun loadSections() {
        viewModelScope.launch {
            db.noteDao().getSections().collectLatest {
                _sections.value = it
            }
        }
    }

    fun selectSection(section: String) {
        _selectedSection.value = section
        viewModelScope.launch {
            db.noteDao().getNotesBySection(section).collectLatest {
                _notes.value = it
            }
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            db.noteDao().deleteById(id)
        }
    }
}
