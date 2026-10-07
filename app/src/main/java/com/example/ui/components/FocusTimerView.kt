package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FocusTimerUiState
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600

@Composable
fun FocusTimerView(
    timerState: FocusTimerUiState,
    onTogglePauseResume: () -> Unit,
    onReset: () -> Unit,
    onComplete: () -> Unit,
    onPresetSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingSecs = timerState.remainingSeconds
    val totalSecs = if (timerState.totalSeconds > 0) timerState.totalSeconds else 1
    val progress = (remainingSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)

    val minutes = remainingSecs / 60
    val seconds = remainingSecs % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    // Breathing pulse scale animation while running
    val infiniteTransition = rememberInfiniteTransition(label = "aura_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (timerState.isRunning) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Aura Focus Space",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timerState.habit?.title ?: "Deep Work & Mindful Focus",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        // Circular Timer Visualizer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp)
        ) {
            // Pulsing Background Aura
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Emerald500.copy(alpha = if (timerState.isRunning) 0.35f else 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Progress Ring
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(230.dp),
                color = Emerald500,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 10.dp
            )

            // Center Countdown Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 52.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (timerState.isRunning) "In Flow state..." else if (timerState.isFinished) "Completed! ✨" else "Ready to Focus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Preset Durations
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Preset Minutes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(5, 10, 15, 25)
                presets.forEach { presetMins ->
                    OutlinedButton(
                        onClick = { onPresetSelected(presetMins) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("preset_button_${presetMins}m")
                    ) {
                        Text("${presetMins}m")
                    }
                }
            }
        }

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Timer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Play / Pause Button
            Button(
                onClick = onTogglePauseResume,
                modifier = Modifier
                    .height(60.dp)
                    .width(130.dp)
                    .testTag("focus_timer_toggle_button"),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Icon(
                    imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (timerState.isRunning) "Pause" else "Start",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (timerState.isRunning) "Pause" else "Start",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Finish Early Button
            IconButton(
                onClick = onComplete,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Emerald500.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete Focus",
                    tint = Emerald600
                )
            }
        }
    }
}
