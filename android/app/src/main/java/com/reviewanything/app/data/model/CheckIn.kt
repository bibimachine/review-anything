package com.reviewanything.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkins")
data class CheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val checkinDate: String = "",  // YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)
