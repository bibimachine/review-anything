package com.reviewanything.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.reviewanything.app.data.model.CheckIn
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM checkins WHERE checkinDate = :date LIMIT 1")
    suspend fun getCheckInByDate(date: String): CheckIn?

    @Query("SELECT * FROM checkins ORDER BY checkinDate DESC")
    fun getAllCheckIns(): Flow<List<CheckIn>>

    @Query("SELECT COUNT(*) FROM checkins")
    fun getTotalCount(): Flow<Int>

    @Insert
    suspend fun insert(checkIn: CheckIn)

    @Query("SELECT * FROM checkins WHERE strftime('%Y-%m', checkinDate) = :yearMonth")
    suspend fun getCheckInsByMonth(yearMonth: String): List<CheckIn>
}
