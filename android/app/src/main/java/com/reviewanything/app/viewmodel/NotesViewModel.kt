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

    private val _allNotes = MutableStateFlow<Map<String, List<Note>>>(emptyMap())
    val allNotes: StateFlow<Map<String, List<Note>>> = _allNotes

    private val _sections = MutableStateFlow<List<String>>(emptyList())
    val sections: StateFlow<List<String>> = _sections

    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            db.noteDao().getAllNotes().collectLatest { notes ->
                val grouped = notes.groupBy { it.section }
                    .mapValues { (_, list) -> list.sortedBy { it.fileName } }
                _allNotes.value = grouped
                _sections.value = grouped.keys.sorted()
            }
        }
    }

    fun selectNote(note: Note?) {
        _selectedNote.value = note
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            db.noteDao().deleteById(id)
            if (_selectedNote.value?.id == id) {
                _selectedNote.value = null
            }
            loadAll()
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
            loadAll()
        }
        return true
    }
}
