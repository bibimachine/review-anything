package com.reviewanything.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_items",
    foreignKeys = [
        ForeignKey(
            entity = Chunk::class,
            parentColumns = ["id"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chunkId"])]
)
data class ReviewItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val chunkId: Int = 0,
    val question: String = "",
    val answer: String = "",
    val isHard: Boolean = false,
    val reviewCount: Int = 0,
    val nextReviewAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val llmFailed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
