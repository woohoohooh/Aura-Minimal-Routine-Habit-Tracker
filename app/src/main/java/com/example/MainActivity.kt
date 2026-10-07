package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AuraDatabase
import com.example.data.AuraRepository
import com.example.ui.AuraViewModel
import com.example.ui.AuraViewModelFactory
import com.example.ui.components.*
import com.example.ui.theme.AuraTheme
import com.example.ui.theme.Emerald600

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val database = remember { AuraDatabase.getDatabase(context) }
            val repository = remember { AuraRepository(database.auraDao()) }
            val viewModel: AuraViewModel = viewModel(factory = AuraViewModelFactory(repository))

            AuraTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

enum class AuraTab(val label: String, val selectedIcon: Any, val unselectedIcon: Any) {
    RITUALS("Rituals", Icons.Default.CheckCircle, Icons.Outlined.RadioButtonUnchecked),
    FOCUS("Focus", Icons.Default.Timer, Icons.Outlined.Timer),
    INSIGHTS("Insights", Icons.Default.Analytics, Icons.Outlined.Analytics),
    JOURNAL("Journal", Icons.Default.Book, Icons.Outlined.Book)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AuraViewModel) {
    var selectedTab by remember { mutableStateOf(AuraTab.RITUALS) }
    var showAddDialog by remember { mutableStateOf(false) }

    val habitsState by viewModel.habitsUiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val progress by viewModel.completionProgress.collectAsStateWithLifecycle()
    val currentReflection by viewModel.currentReflection.collectAsStateWithLifecycle()
    val allReflections by viewModel.allReflections.collectAsStateWithLifecycle()
    val recent14DaysLogs by viewModel.recent14DaysLogs.collectAsStateWithLifecycle()
    val focusTimerState by viewModel.focusTimerState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                AuraTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AuraTab.RITUALS -> if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked
                                    AuraTab.FOCUS -> if (isSelected) Icons.Default.Timer else Icons.Outlined.Timer
                                    AuraTab.INSIGHTS -> if (isSelected) Icons.Default.BarChart else Icons.Outlined.BarChart
                                    AuraTab.JOURNAL -> if (isSelected) Icons.Default.EditNote else Icons.Outlined.EditNote
                                },
                                contentDescription = tab.label
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == AuraTab.RITUALS) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Emerald600,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("add_habit_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AuraTab.RITUALS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RoutineHeader(
                            selectedDate = selectedDate,
                            onDateSelected = { viewModel.setSelectedDate(it) },
                            selectedCategory = selectedCategory,
                            onCategorySelected = { viewModel.setSelectedCategory(it) }
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                ProgressHeroCard(
                                    progress = progress,
                                    totalHabitsCount = habitsState.size,
                                    completedHabitsCount = habitsState.count { it.isCompleted }
                                )
                            }

                            if (habitsState.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No rituals found for this category. Tap + to create one!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(
                                    items = habitsState,
                                    key = { it.habit.id }
                                ) { habitItem ->
                                    HabitCard(
                                        item = habitItem,
                                        onToggleCompletion = { viewModel.toggleHabitCompletion(habitItem) },
                                        onUpdateProgress = { delta -> viewModel.updateHabitProgress(habitItem, delta) },
                                        onStartFocusTimer = {
                                            viewModel.startFocusTimerForHabit(habitItem.habit)
                                            selectedTab = AuraTab.FOCUS
                                        },
                                        onDeleteHabit = { viewModel.deleteHabit(habitItem.habit.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                AuraTab.FOCUS -> {
                    FocusTimerView(
                        timerState = focusTimerState,
                        onTogglePauseResume = { viewModel.toggleTimerPauseResume() },
                        onReset = { viewModel.resetTimer() },
                        onComplete = { viewModel.completeFocusSession() },
                        onPresetSelected = { mins -> viewModel.startStandaloneFocusTimer(mins) }
                    )
                }

                AuraTab.INSIGHTS -> {
                    AnalyticsView(
                        habits = habitsState,
                        recentLogs = recent14DaysLogs
                    )
                }

                AuraTab.JOURNAL -> {
                    ReflectionView(
                        selectedDate = selectedDate,
                        currentReflection = currentReflection,
                        allReflections = allReflections,
                        onSaveReflection = { mood, note -> viewModel.saveReflection(mood, note) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddHabitDialog(
                onDismiss = { showAddDialog = false },
                onAddHabit = { title, category, iconName, colorHex, targetValue, unit, isFocusHabit, defaultTimerMinutes ->
                    viewModel.addNewHabit(
                        title, category, iconName, colorHex, targetValue, unit, isFocusHabit, defaultTimerMinutes
                    )
                }
            )
        }
    }
}
