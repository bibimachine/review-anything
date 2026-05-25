package com.reviewanything.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.reviewanything.app.data.model.Config
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configs LIMIT 1")
    fun getConfig(): Flow<Config?>

    @Query("SELECT * FROM configs LIMIT 1")
    suspend fun getConfigSync(): Config?

    @Insert
    suspend fun insert(config: Config)

    @Update
    suspend fun update(config: Config)

    @Query("DELETE FROM configs")
    suspend fun deleteAll()
}
