package com.leteam.locked.ui.screens.insights

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leteam.locked.ui.screens.insights.components.LineChart
import java.text.SimpleDateFormat
import java.util.Locale

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
            color = Color.DarkGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBackClick: () -> Unit = {},
    viewModel: InsightsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Insights", fontWeight = FontWeight.Black, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.padding(paddingValues),
            color = Color.White
        ) {
            Crossfade(targetState = state, label = "insights") { uiState ->
                when (uiState) {
                    is InsightsUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.Black)
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
        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(24.dp))

        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = uiState.insights.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )

                Spacer(Modifier.height(16.dp))

                val values = uiState.insights.trend.map { it.value }
                if (values.size >= 2) {
                    LineChart(
                        points = values,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        lineColor = Color.Black,
                        gridColor = Color(0xFFE0E0E0)
                    )

                    Spacer(Modifier.height(12.dp))

                    val start = uiState.insights.trend.firstOrNull()?.date
                    val end = uiState.insights.trend.lastOrNull()?.date
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = start?.let(df::format) ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = end?.let(df::format) ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Color(0xFFF5F5F5),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not enough data yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HighlightCard(
                title = "ALL-TIME PR",
                value = if (uiState.insights.prWeight > 0) "${uiState.insights.prWeight} lbs" else "—",
                subtitle = uiState.insights.prDate?.let(df::format) ?: "",
                modifier = Modifier.weight(1f)
            )
            HighlightCard(
                title = "BEST SESSION",
                value = if (uiState.insights.bestSessionVolume > 0) "${uiState.insights.bestSessionVolume.toInt()} vol" else "—",
                subtitle = "${uiState.insights.totalSessions} sessions",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.dp))
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
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFAAAAAA),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA)
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
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Text(selected, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            exercises.forEach { ex ->
                DropdownMenuItem(
                    text = { Text(ex, color = Color.Black, fontWeight = FontWeight.Medium) },
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
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Text(selected.label, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            InsightsRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.label, color = Color.Black, fontWeight = FontWeight.Medium) },
                    onClick = {
                        expanded = false
                        onSelected(range)
                    }
                )
            }
        }
    }
}