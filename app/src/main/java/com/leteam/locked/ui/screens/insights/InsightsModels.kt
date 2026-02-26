package com.leteam.locked.ui.screens.insights

import java.util.Date

enum class InsightsRange(val label: String, val days: Int?) {
    D7("7D", 7),
    D30("30D", 30),
    D90("90D", 90),
    ALL("All", null)
}

data class ExerciseTrendPoint(
    val date: Date,
    val value: Double
)

data class ExerciseInsights(
    val exerciseName: String,
    val trend: List<ExerciseTrendPoint>,
    val prWeight: Double,
    val prDate: Date?,
    val bestSessionVolume: Double,
    val totalSessions: Int
)

sealed interface InsightsUiState {
    data object Loading : InsightsUiState
    data class Loaded(
        val exercises: List<String>,
        val selectedExercise: String,
        val range: InsightsRange,
        val insights: ExerciseInsights
    ) : InsightsUiState

    data class Empty(
        val message: String
    ) : InsightsUiState

    data class Error(
        val message: String
    ) : InsightsUiState
}
