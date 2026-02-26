package com.leteam.locked.ui.screens.insights

import androidx.lifecycle.ViewModel
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.workout.Exercise
import com.leteam.locked.workout.Workout
import com.leteam.locked.workout.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date

class InsightsViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var cached: List<LoggedExercise> = emptyList()
    private var selectedExercise: String? = null
    private var selectedRange: InsightsRange = InsightsRange.D30

    fun load() {
        val userId = FirebaseProvider.auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = InsightsUiState.Error("User not logged in")
            return
        }

        _uiState.value = InsightsUiState.Loading

        workoutRepository.getWorkouts(userId) { workoutsResult ->
            val workouts = workoutsResult.getOrElse {
                _uiState.value = InsightsUiState.Error(it.message ?: "Failed to load workouts")
                return@getWorkouts
            }

            if (workouts.isEmpty()) {
                _uiState.value = InsightsUiState.Empty("No workouts yet")
                return@getWorkouts
            }

            loadExercisesForWorkouts(userId, workouts)
        }
    }

    fun setExercise(exerciseName: String) {
        selectedExercise = exerciseName
        publish()
    }

    fun setRange(range: InsightsRange) {
        selectedRange = range
        publish()
    }

    private fun loadExercisesForWorkouts(userId: String, workouts: List<Workout>) {
        val logged = mutableListOf<LoggedExercise>()
        var remaining = workouts.size
        var failed: Exception? = null

        workouts.forEach { workout ->
            workoutRepository.getExercisesForWorkout(userId, workout.id) { exResult ->
                if (failed == null) {
                    exResult
                        .onSuccess { exercises ->
                            exercises.forEach { ex ->
                                logged.add(
                                    LoggedExercise(
                                        workoutDate = workout.workoutDate,
                                        exercise = ex
                                    )
                                )
                            }
                        }
                        .onFailure { e -> failed = e as? Exception ?: Exception(e) }
                }

                remaining -= 1
                if (remaining == 0) {
                    if (failed != null) {
                        _uiState.value = InsightsUiState.Error(failed?.message ?: "Failed to load exercises")
                        return@getExercisesForWorkout
                    }

                    cached = logged
                    if (selectedExercise == null) {
                        selectedExercise = cached
                            .map { it.exercise.name }
                            .distinct()
                            .sorted()
                            .firstOrNull()
                    }
                    publish()
                }
            }
        }
    }

    private fun publish() {
        val exercises = cached.map { it.exercise.name }.distinct().sorted()
        val selected = selectedExercise
        if (exercises.isEmpty() || selected == null) {
            _uiState.value = InsightsUiState.Empty("No exercises yet")
            return
        }

        val filtered = cached
            .filter { it.exercise.name == selected }
            .filter { withinRange(it.workoutDate, selectedRange) }
            .sortedBy { it.workoutDate }

        if (filtered.isEmpty()) {
            _uiState.value = InsightsUiState.Loaded(
                exercises = exercises,
                selectedExercise = selected,
                range = selectedRange,
                insights = ExerciseInsights(
                    exerciseName = selected,
                    trend = emptyList(),
                    prWeight = 0.0,
                    prDate = null,
                    bestSessionVolume = 0.0,
                    totalSessions = 0
                )
            )
            return
        }

        val sessions = filtered.groupBy { dayKey(it.workoutDate) }

        // PR is max weight ever logged for the exercise
        val prEntry = filtered.maxByOrNull { it.exercise.weightAmount }
        val prWeight = prEntry?.exercise?.weightAmount ?: 0.0
        val prDate = prEntry?.workoutDate

        // "Best session" uses total volume: weight * sets * reps
        val bestSessionVolume = sessions.values.maxOf { items ->
            items.sumOf { it.exercise.weightAmount * it.exercise.numberOfSets * it.exercise.repsPerSet }
        }

        // Trend uses daily max weight so the chart stays stable even with multiple sets logged per day.
        val trend = sessions.entries
            .sortedBy { it.key }
            .map { (_, items) ->
                val date = items.maxBy { it.workoutDate }.workoutDate
                val maxWeight = items.maxOf { it.exercise.weightAmount }
                ExerciseTrendPoint(date = date, value = maxWeight)
            }

        _uiState.value = InsightsUiState.Loaded(
            exercises = exercises,
            selectedExercise = selected,
            range = selectedRange,
            insights = ExerciseInsights(
                exerciseName = selected,
                trend = trend,
                prWeight = prWeight,
                prDate = prDate,
                bestSessionVolume = bestSessionVolume,
                totalSessions = sessions.size
            )
        )
    }

    private fun withinRange(date: Date, range: InsightsRange): Boolean {
        val days = range.days ?: return true
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return date.after(cal.time)
    }

    private fun dayKey(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private data class LoggedExercise(
        val workoutDate: Date,
        val exercise: Exercise
    )
}
