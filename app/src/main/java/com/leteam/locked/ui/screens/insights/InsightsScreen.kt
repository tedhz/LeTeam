package com.leteam.locked.ui.screens.insights

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leteam.locked.ui.screens.insights.components.LineChart
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // No entrypoint yet
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface {
        Crossfade(targetState = state, label = "insights") { uiState ->
            when (uiState) {
                is InsightsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is InsightsUiState.Empty -> {
                    CenterMessage(message = uiState.message)
                }

                is InsightsUiState.Error -> {
                    CenterMessage(message = uiState.message)
                }

                is InsightsUiState.Loaded -> {
                    InsightsContent(
                        uiState = uiState,
                        onExerciseSelected = viewModel::setExercise,
                        onRangeSelected = viewModel::setRange
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsContent(
    uiState: InsightsUiState.Loaded,
    onExerciseSelected: (String) -> Unit,
    onRangeSelected: (InsightsRange) -> Unit
) {
    val scroll = rememberScrollState()
    val df = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text = "Insights",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExerciseDropdown(
                exercises = uiState.exercises,
                selected = uiState.selectedExercise,
                onSelected = onExerciseSelected,
                modifier = Modifier.weight(1f)
            )

            RangeDropdown(
                selected = uiState.range,
                onSelected = onRangeSelected
            )
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = uiState.insights.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(10.dp))

                val values = uiState.insights.trend.map { it.value }
                if (values.size >= 2) {
                    LineChart(
                        points = values,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        lineColor = MaterialTheme.colorScheme.primary,
                        gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    Spacer(Modifier.height(10.dp))

                    val start = uiState.insights.trend.firstOrNull()?.date
                    val end = uiState.insights.trend.lastOrNull()?.date
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = start?.let(df::format) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = end?.let(df::format) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not enough data yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HighlightCard(
                title = "All‑time PR",
                value = if (uiState.insights.prWeight > 0) "${uiState.insights.prWeight}" else "—",
                subtitle = uiState.insights.prDate?.let(df::format) ?: "",
                modifier = Modifier.weight(1f)
            )
            HighlightCard(
                title = "Best session",
                value = if (uiState.insights.bestSessionVolume > 0) "${uiState.insights.bestSessionVolume.toInt()} vol" else "—",
                subtitle = "${uiState.insights.totalSessions} sessions",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HighlightCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExerciseDropdown(
    exercises: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { ex ->
                DropdownMenuItem(
                    text = { Text(ex) },
                    onClick = {
                        expanded = false
                        onSelected(ex)
                    }
                )
            }
        }
    }
}

@Composable
private fun RangeDropdown(
    selected: InsightsRange,
    onSelected: (InsightsRange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(selected.label)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            InsightsRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.label) },
                    onClick = {
                        expanded = false
                        onSelected(range)
                    }
                )
            }
        }
    }
}

@Composable
private fun CenterMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
