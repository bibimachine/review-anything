package com.reviewanything.app.data.repository

import com.reviewanything.app.data.db.NoteDao
import com.reviewanything.app.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    fun getSections(): Flow<List<String>> = noteDao.getSections()
    fun getNotesBySection(section: String): Flow<List<Note>> = noteDao.getNotesBySection(section)
    suspend fun deleteNote(id: Int) = noteDao.deleteById(id)
}
