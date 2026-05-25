package com.reviewanything.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filePath: String = "",
    val fileName: String = "",
    val section: String = "",
    val content: String = "",
    val contentHash: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
