package com.reviewanything.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configs")
data class Config(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val apiBaseUrl: String? = null,
    val apiKey: String? = null,
    val modelName: String = "deepseek-v4-pro",
    val dailyReviewCount: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)
