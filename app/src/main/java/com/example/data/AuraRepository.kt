package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AuraRepository(private val dao: AuraDao) {

    val allHabits: Flow<List<Habit>> = dao.getAllActiveHabits()

    fun getLogsForDate(date: String): Flow<List<HabitLog>> = dao.getLogsForDate(date)

    fun getLogsForDateRange(startDate: String, endDate: String): Flow<List<HabitLog>> =
        dao.getLogsForDateRange(startDate, endDate)

    fun getReflectionForDate(date: String): Flow<DailyReflection?> = dao.getReflectionForDate(date)

    fun getAllReflections(): Flow<List<DailyReflection>> = dao.getAllReflections()

    suspend fun insertHabit(habit: Habit): Long = dao.insertHabit(habit)

    suspend fun deleteHabit(habitId: Int) = dao.deleteHabit(habitId)

    suspend fun toggleHabitCompletion(habit: Habit, date: String, isCurrentlyCompleted: Boolean) {
        if (isCurrentlyCompleted) {
            dao.deleteLog(habit.id, date)
        } else {
            val log = HabitLog(
                habitId = habit.id,
                date = date,
                completedValue = habit.targetValue,
                isCompleted = true
            )
            dao.insertOrUpdateLog(log)
        }
    }

    suspend fun updateHabitProgress(habit: Habit, date: String, newValue: Int) {
        val isCompleted = newValue >= habit.targetValue
        if (newValue <= 0) {
            dao.deleteLog(habit.id, date)
        } else {
            val log = HabitLog(
                habitId = habit.id,
                date = date,
                completedValue = newValue,
                isCompleted = isCompleted
            )
            dao.insertOrUpdateLog(log)
        }
    }

    suspend fun saveReflection(date: String, mood: String, note: String) {
        dao.saveReflection(DailyReflection(date = date, mood = mood, note = note))
    }

    suspend fun calculateStreak(habitId: Int): Int {
        val completedLogs = dao.getAllCompletedLogsForHabit(habitId)
        if (completedLogs.isEmpty()) return 0

        val completedDates = completedLogs.map { it.date }.toSet()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        var today = LocalDate.now()
        var streak = 0

        // Check if today is completed, or if yesterday was completed (to allow checking streak today)
        var checkDate = today
        if (!completedDates.contains(checkDate.format(formatter))) {
            checkDate = checkDate.minusDays(1)
        }

        while (completedDates.contains(checkDate.format(formatter))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    suspend fun seedInitialDataIfEmpty() {
        val habits = allHabits.first()
        if (habits.isEmpty()) {
            val defaults = listOf(
                Habit(
                    title = "Morning Glass of Water",
                    category = "Morning",
                    iconName = "water_drop",
                    colorHex = "#0D9488",
                    targetValue = 500,
                    unit = "ml"
                ),
                Habit(
                    title = "10-Min Mindful Focus",
                    category = "Morning",
                    iconName = "self_improvement",
                    colorHex = "#F59E0B",
                    targetValue = 10,
                    unit = "mins",
                    isFocusHabit = true,
                    defaultTimerMinutes = 10
                ),
                Habit(
                    title = "Daily Walk in Nature",
                    category = "Afternoon",
                    iconName = "directions_walk",
                    colorHex = "#10B981",
                    targetValue = 1,
                    unit = "walk"
                ),
                Habit(
                    title = "Evening Reading & Unwind",
                    category = "Evening",
                    iconName = "menu_book",
                    colorHex = "#6366F1",
                    targetValue = 15,
                    unit = "mins",
                    isFocusHabit = true,
                    defaultTimerMinutes = 15
                )
            )

            for (habit in defaults) {
                dao.insertHabit(habit)
            }
        }
    }
}
