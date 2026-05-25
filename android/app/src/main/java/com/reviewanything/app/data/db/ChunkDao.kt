package com.reviewanything.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.reviewanything.app.data.model.Chunk
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {
    @Query("SELECT * FROM chunks WHERE noteId = :noteId")
    fun getChunksByNoteId(noteId: Int): Flow<List<Chunk>>

    @Insert
    suspend fun insert(chunk: Chunk): Long

    @Query("DELETE FROM chunks WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Int)
}
