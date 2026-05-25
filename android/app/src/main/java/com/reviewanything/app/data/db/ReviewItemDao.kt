package com.reviewanything.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.reviewanything.app.data.model.ReviewItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewItemDao {
    @Query("""
        SELECT * FROM review_items 
        WHERE nextReviewAt <= :now 
        ORDER BY nextReviewAt ASC 
        LIMIT :count
    """)
    suspend fun getDueItems(now: Long, count: Int): List<ReviewItem>

    @Query("SELECT COUNT(*) FROM review_items WHERE nextReviewAt <= :now")
    fun getDueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM review_items WHERE chunkId = :chunkId")
    fun getItemsByChunkId(chunkId: Int): Flow<List<ReviewItem>>

    @Insert
    suspend fun insert(item: ReviewItem): Long

    @Update
    suspend fun update(item: ReviewItem)

    @Query("DELETE FROM review_items WHERE chunkId IN (SELECT id FROM chunks WHERE noteId = :noteId)")
    suspend fun deleteByNoteId(noteId: Int)
}
