package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuraRepository
import com.example.data.DailyReflection
import com.example.data.Habit
import com.example.data.HabitItemUiState
import com.example.data.HabitLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class FocusTimerUiState(
    val habit: Habit? = null,
    val totalSeconds: Int = 600,
    val remainingSeconds: Int = 600,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class AuraViewModel(private val repository: AuraRepository) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _focusTimerState = MutableStateFlow(FocusTimerUiState())
    val focusTimerState: StateFlow<FocusTimerUiState> = _focusTimerState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // Combine habits and logs for selected date
    val habitsUiState: StateFlow<List<HabitItemUiState>> = combine(
        repository.allHabits,
        _selectedDate.flatMapLatest { date -> repository.getLogsForDate(date) },
        _selectedCategory
    ) { habits, logs, category ->
        val logMap = logs.associateBy { it.habitId }
        val filteredHabits = if (category == "All") habits else habits.filter { it.category.equals(category, ignoreCase = true) }

        filteredHabits.map { habit ->
            val log = logMap[habit.id]
            val completedValue = log?.completedValue ?: 0
            val isCompleted = log?.isCompleted ?: false
            val streak = repository.calculateStreak(habit.id)

            HabitItemUiState(
                habit = habit,
                completedValue = completedValue,
                isCompleted = isCompleted,
                currentStreak = streak
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Completion percentage for selected date
    val completionProgress: StateFlow<Float> = habitsUiState.map { items ->
        if (items.isEmpty()) 0f
        else {
            val completedCount = items.count { it.isCompleted }
            completedCount.toFloat() / items.size.toFloat()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    // Current date reflection
    val currentReflection: StateFlow<DailyReflection?> = _selectedDate.flatMapLatest { date ->
        repository.getReflectionForDate(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // All reflections for journal tab
    val allReflections: StateFlow<List<DailyReflection>> = repository.getAllReflections().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Last 14 days logs for visual analytics grid
    val recent14DaysLogs: StateFlow<List<HabitLog>> = flow {
        val endDate = LocalDate.now().format(dateFormatter)
        val startDate = LocalDate.now().minusDays(14).format(dateFormatter)
        emitAll(repository.getLogsForDateRange(startDate, endDate))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleHabitCompletion(item: HabitItemUiState) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(
                habit = item.habit,
                date = _selectedDate.value,
                isCurrentlyCompleted = item.isCompleted
            )
        }
    }

    fun updateHabitProgress(item: HabitItemUiState, delta: Int) {
        viewModelScope.launch {
            val newValue = (item.completedValue + delta).coerceAtLeast(0)
            repository.updateHabitProgress(
                habit = item.habit,
                date = _selectedDate.value,
                newValue = newValue
            )
        }
    }

    fun addNewHabit(
        title: String,
        category: String,
        iconName: String,
        colorHex: String,
        targetValue: Int,
        unit: String,
        isFocusHabit: Boolean,
        defaultTimerMinutes: Int
    ) {
        viewModelScope.launch {
            val habit = Habit(
                title = title,
                category = category,
                iconName = iconName,
                colorHex = colorHex,
                targetValue = targetValue,
                unit = unit,
                isFocusHabit = isFocusHabit,
                defaultTimerMinutes = defaultTimerMinutes
            )
            repository.insertHabit(habit)
        }
    }

    fun deleteHabit(habitId: Int) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun saveReflection(mood: String, note: String) {
        viewModelScope.launch {
            repository.saveReflection(
                date = _selectedDate.value,
                mood = mood,
                note = note
            )
        }
    }

    // Focus Timer
    fun startFocusTimerForHabit(habit: Habit) {
        timerJob?.cancel()
        val minutes = if (habit.defaultTimerMinutes > 0) habit.defaultTimerMinutes else 10
        val totalSecs = minutes * 60

        _focusTimerState.value = FocusTimerUiState(
            habit = habit,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs,
            isRunning = true,
            isFinished = false
        )

        runTimerLoop()
    }

    fun startStandaloneFocusTimer(minutes: Int) {
        timerJob?.cancel()
        val totalSecs = minutes * 60
        _focusTimerState.value = FocusTimerUiState(
            habit = null,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs,
            isRunning = true,
            isFinished = false
        )
        runTimerLoop()
    }

    fun toggleTimerPauseResume() {
        val current = _focusTimerState.value
        if (current.isRunning) {
            timerJob?.cancel()
            _focusTimerState.value = current.copy(isRunning = false)
        } else if (current.remainingSeconds > 0) {
            _focusTimerState.value = current.copy(isRunning = true)
            runTimerLoop()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        val current = _focusTimerState.value
        _focusTimerState.value = current.copy(
            remainingSeconds = current.totalSeconds,
            isRunning = false,
            isFinished = false
        )
    }

    fun completeFocusSession() {
        timerJob?.cancel()
        val current = _focusTimerState.value
        val habit = current.habit
        if (habit != null) {
            viewModelScope.launch {
                repository.updateHabitProgress(
                    habit = habit,
                    date = _selectedDate.value,
                    newValue = habit.targetValue
                )
            }
        }
        _focusTimerState.value = current.copy(
            remainingSeconds = 0,
            isRunning = false,
            isFinished = true
        )
    }

    private fun runTimerLoop() {
        timerJob = viewModelScope.launch {
            while (_focusTimerState.value.remainingSeconds > 0 && _focusTimerState.value.isRunning) {
                delay(1000L)
                val remaining = _focusTimerState.value.remainingSeconds - 1
                if (remaining <= 0) {
                    completeFocusSession()
                } else {
                    _focusTimerState.value = _focusTimerState.value.copy(remainingSeconds = remaining)
                }
            }
        }
    }
}

class AuraViewModelFactory(private val repository: AuraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
