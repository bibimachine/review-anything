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
            db.noteDao().getSections().collectLatest { list ->
                // 也包含占位符板块
                val placeholderSections = db.noteDao().getPlaceholderSections()
                _sections.value = (list + placeholderSections).distinct().sorted()
            }
        }
    }

    fun selectSection(section: String) {
        _selectedSection.value = section
        viewModelScope.launch {
            db.noteDao().getNotesBySection(section).collectLatest {
                _notes.value = it.filter { note -> note.fileName != "_placeholder" }
            }
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            db.noteDao().deleteById(id)
            loadSections()
        }
    }

    fun createSection(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_sections.value.contains(trimmed)) return false

        viewModelScope.launch {
            db.noteDao().insert(
                Note(
                    filePath = "",
                    fileName = "_placeholder",
                    section = trimmed,
                    content = "",
                    contentHash = ""
                )
            )
            loadSections()
        }
        return true
    }
}
