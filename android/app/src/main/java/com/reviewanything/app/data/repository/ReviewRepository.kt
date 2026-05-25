package com.reviewanything.app.data.repository

import com.reviewanything.app.data.db.ReviewItemDao
import com.reviewanything.app.data.model.ReviewItem
import kotlinx.coroutines.flow.Flow
import java.util.*
import java.util.concurrent.TimeUnit

class ReviewRepository(private val reviewItemDao: ReviewItemDao) {

    suspend fun getDueItems(count: Int): List<ReviewItem> {
        return reviewItemDao.getDueItems(System.currentTimeMillis(), count)
    }

    fun getDueCount(): Flow<Int> {
        return reviewItemDao.getDueCount(System.currentTimeMillis())
    }

    suspend fun markRemembered(item: ReviewItem) {
        val newCount = item.reviewCount + 1
        val intervalDays = when (newCount) {
            1 -> 1
            2 -> 3
            3 -> 7
            4 -> 14
            else -> 30
        }
        val nextReview = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(intervalDays.toLong())
        reviewItemDao.update(
            item.copy(
                reviewCount = newCount,
                nextReviewAt = nextReview,
                lastReviewedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markForgotten(item: ReviewItem) {
        reviewItemDao.update(
            item.copy(
                isHard = true,
                nextReviewAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1),
                lastReviewedAt = System.currentTimeMillis()
            )
        )
    }
}
