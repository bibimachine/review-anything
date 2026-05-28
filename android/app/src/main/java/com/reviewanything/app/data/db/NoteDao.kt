package com.reviewanything.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.reviewanything.app.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT DISTINCT section FROM notes")
    fun getSections(): Flow<List<String>>

    @Query("SELECT * FROM notes WHERE section = :section")
    fun getNotesBySection(section: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT section FROM notes WHERE fileName = '_placeholder'")
    suspend fun getPlaceholderSections(): List<String>

    @Query("SELECT * FROM notes WHERE fileName != '_placeholder' ORDER BY section, fileName")
    fun getAllNotes(): Flow<List<Note>>
}
