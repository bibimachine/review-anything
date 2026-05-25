package com.reviewanything.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Chunk(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val noteId: Int = 0,
    val content: String = "",
    val contentHash: String = "",
    val headingPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
