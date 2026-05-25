package com.reviewanything.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reviewanything.app.data.model.Config
import com.reviewanything.app.data.model.Note
import com.reviewanything.app.data.model.Chunk
import com.reviewanything.app.data.model.ReviewItem
import com.reviewanything.app.data.model.CheckIn

@Database(
    entities = [Config::class, Note::class, Chunk::class, ReviewItem::class, CheckIn::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun noteDao(): NoteDao
    abstract fun chunkDao(): ChunkDao
    abstract fun reviewItemDao(): ReviewItemDao
    abstract fun checkInDao(): CheckInDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "review_anything.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
