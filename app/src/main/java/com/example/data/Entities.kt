package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "Morning", "Afternoon", "Evening", "Anytime"
    val iconName: String, // e.g. "water_drop", "wb_sunny", "fitness_center", "menu_book", "self_improvement", "bedtime", "directions_walk"
    val colorHex: String, // e.g. "#0D9488", "#F59E0B", "#6366F1", "#EC4899"
    val targetValue: Int = 1,
    val unit: String = "times", // "times", "mins", "ml", "pages"
    val isFocusHabit: Boolean = false,
    val defaultTimerMinutes: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val date: String, // ISO date string "YYYY-MM-DD"
    val completedValue: Int = 0,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_reflections")
data class DailyReflection(
    @PrimaryKey val date: String, // ISO date string "YYYY-MM-DD"
    val mood: String, // "GRATEFUL", "CALM", "ENERGIZED", "FOCUSED", "PEACEFUL"
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class HabitItemUiState(
    val habit: Habit,
    val completedValue: Int,
    val isCompleted: Boolean,
    val currentStreak: Int
)
