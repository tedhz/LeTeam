package com.leteam.locked.ui.screens.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale
import com.leteam.locked.workout.Exercise
import com.leteam.locked.ui.screens.workouts.WorkoutFeedItem
@Composable
fun WorkoutsFeedScreen(
    onWorkoutOpen: () -> Unit,
    viewModel: WorkoutsFeedViewModel = viewModel(factory = WorkoutsFeedViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Top "box button" style card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onWorkoutOpen,
                    colors = CardDefaults.elevatedCardColors()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "My Workouts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Create / view your own workouts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Text(
                        text = "Loading workouts...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            uiState.errorMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            items(uiState.feedItems, key = { it.workout.id }) { item ->
                WorkoutFeedCard(item = item)
            }
        }
    }
}

@Composable
private fun WorkoutFeedCard(item: WorkoutFeedItem) {
    val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val workoutDate = df.format(item.workout.workoutDate)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row: user name + date
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.userDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = workoutDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Exercises sub-box
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Exercises",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.exercises.isEmpty()) {
                        Text(
                            text = "No exercises logged.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        item.exercises.forEach { ex ->
                            ExerciseRow(ex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(ex: Exercise) {
    // Looks like: Bench Press • 4 x 8 @ 135
    val detail = "${ex.numberOfSets} x ${ex.repsPerSet} @ ${trimWeight(ex.weightAmount)}"

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = ex.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun trimWeight(w: Double): String {
    val asInt = w.toInt()
    return if (w == asInt.toDouble()) asInt.toString() else w.toString()
}