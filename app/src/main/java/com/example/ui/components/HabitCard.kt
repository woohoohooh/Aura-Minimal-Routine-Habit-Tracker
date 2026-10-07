package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitItemUiState
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500

@Composable
fun HabitCard(
    item: HabitItemUiState,
    onToggleCompletion: () -> Unit,
    onUpdateProgress: (Int) -> Unit,
    onStartFocusTimer: () -> Unit,
    onDeleteHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = item.habit
    val isCompleted = item.isCompleted

    var showMenu by remember { mutableStateOf(false) }

    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "check_scale"
    )

    val cardBgColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "card_bg"
    )

    val habitColor = parseColorHex(habit.colorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("habit_card_${habit.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkmark Circle Button
            IconButton(
                onClick = onToggleCompletion,
                modifier = Modifier
                    .size(48.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) Emerald500 else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("habit_check_button_${habit.id}")
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.Check else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Toggle Completion",
                    tint = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Habit Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(habitColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = habit.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = habitColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Streak Flame Badge
                    if (item.currentStreak > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Amber500.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Amber500,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${item.currentStreak}d streak",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Amber500
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (habit.targetValue > 1) "${item.completedValue} / ${habit.targetValue} ${habit.unit}" else habit.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Numeric Target Adjusters or Focus Timer Button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (habit.targetValue > 1 && !isCompleted) {
                    IconButton(
                        onClick = { onUpdateProgress(-1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = "Decrease Progress",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onUpdateProgress(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Increase Progress",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (habit.isFocusHabit && !isCompleted) {
                    IconButton(
                        onClick = onStartFocusTimer,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("habit_focus_timer_button_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Start Focus Timer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete Habit", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteHabit()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

fun parseColorHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Emerald500
    }
}

fun getIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "water_drop" -> Icons.Default.WaterDrop
        "wb_sunny" -> Icons.Default.WbSunny
        "fitness_center" -> Icons.Default.FitnessCenter
        "menu_book" -> Icons.Default.MenuBook
        "self_improvement" -> Icons.Default.SelfImprovement
        "bedtime" -> Icons.Default.Bedtime
        "directions_walk" -> Icons.Default.DirectionsWalk
        else -> Icons.Default.Check
    }
}
