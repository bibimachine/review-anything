package com.reviewanything.app

import android.app.Application
import com.reviewanything.app.data.db.AppDatabase

class ReviewAnythingApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
