package com.iamconanpeter.swipeloom

import android.content.SharedPreferences
import java.time.LocalDate

class LoomProgressManager(private val prefs: SharedPreferences) {
    data class ProgressSnapshot(
        val bestStars: Int,
        val streakDays: Int,
        val totalStars: Int,
        val improved: Boolean
    )

    fun recordWin(puzzleId: String, stars: Int, todayEpochDay: Long = LocalDate.now().toEpochDay()): ProgressSnapshot {
        val starKey = "best_stars_$puzzleId"
        val bestBefore = prefs.getInt(starKey, 0)
        val newBest = maxOf(bestBefore, stars)
        val improved = newBest > bestBefore

        val lastWinDay = prefs.getLong(KEY_LAST_WIN_DAY, Long.MIN_VALUE)
        val streakBefore = prefs.getInt(KEY_STREAK_DAYS, 0)
        val streakNow = when {
            lastWinDay == todayEpochDay -> streakBefore
            lastWinDay == todayEpochDay - 1L -> streakBefore + 1
            else -> 1
        }

        val totalStars = prefs.getInt(KEY_TOTAL_STARS, 0) + stars

        prefs.edit()
            .putInt(starKey, newBest)
            .putInt(KEY_STREAK_DAYS, streakNow)
            .putLong(KEY_LAST_WIN_DAY, todayEpochDay)
            .putInt(KEY_TOTAL_STARS, totalStars)
            .apply()

        return ProgressSnapshot(
            bestStars = newBest,
            streakDays = streakNow,
            totalStars = totalStars,
            improved = improved
        )
    }

    fun currentStreak(): Int = prefs.getInt(KEY_STREAK_DAYS, 0)

    fun totalStars(): Int = prefs.getInt(KEY_TOTAL_STARS, 0)

    fun bestStars(puzzleId: String): Int = prefs.getInt("best_stars_$puzzleId", 0)

    companion object {
        private const val KEY_STREAK_DAYS = "streak_days"
        private const val KEY_LAST_WIN_DAY = "last_win_day"
        private const val KEY_TOTAL_STARS = "total_stars"
    }
}
