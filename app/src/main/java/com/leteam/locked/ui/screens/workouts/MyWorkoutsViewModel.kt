package com.leteam.locked.ui.screens.workouts

import androidx.lifecycle.ViewModel
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.workout.Exercise
import com.leteam.locked.workout.Workout
import com.leteam.locked.workout.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class MyWorkoutsViewModel(
    private val repository: WorkoutRepository = WorkoutRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val userId: String?
        get() = FirebaseProvider.auth.currentUser?.uid

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    private val _selectedWorkout = MutableStateFlow<Workout?>(null)
    val selectedWorkout: StateFlow<Workout?> = _selectedWorkout.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _availableExerciseNames = MutableStateFlow<List<String>>(emptyList())
    val availableExerciseNames: StateFlow<List<String>> = _availableExerciseNames.asStateFlow()

    init {
        loadWorkouts()
    }

    fun loadWorkouts() {
        userId?.let { uid ->
            repository.getWorkouts(uid) { result ->
                result.onSuccess { loadedWorkouts ->
                    _workouts.value = loadedWorkouts
                    loadAvailableExerciseNames(loadedWorkouts)
                }
            }
        }
    }

    private fun loadAvailableExerciseNames(workoutsList: List<Workout>) {
        userId?.let { uid ->
            val names = mutableSetOf<String>()
            var pending = workoutsList.size

            if (pending == 0) {
                _availableExerciseNames.value = emptyList()
                return
            }

            workoutsList.forEach { w ->
                repository.getExercisesForWorkout(uid, w.id) { result ->
                    result.onSuccess { exList ->
                        names.addAll(exList.map { it.name })
                    }
                    pending--
                    if (pending == 0) {
                        _availableExerciseNames.value = names.sorted()
                    }
                }
            }
        }
    }

    fun createWorkout() {
        userId?.let { uid ->
            repository.createWorkout(uid, Date()) { result ->
                result.onSuccess { loadWorkouts() }
            }
        }
    }

    fun selectWorkout(workout: Workout) {
        _selectedWorkout.value = workout
        loadExercises(workout.id)
    }

    fun clearSelectedWorkout() {
        _selectedWorkout.value = null
        _exercises.value = emptyList()
    }

    private fun loadExercises(workoutId: String) {
        userId?.let { uid ->
            repository.getExercisesForWorkout(uid, workoutId) { result ->
                result.onSuccess { _exercises.value = it }
            }
        }
    }

    fun addExercise(name: String, sets: Int, reps: Int, weight: Double) {
        val workoutId = _selectedWorkout.value?.id ?: return
        userId?.let { uid ->
            val exercise = Exercise(name = name, numberOfSets = sets, repsPerSet = reps, weightAmount = weight)
            repository.addExercisesToWorkout(uid, workoutId, listOf(exercise)) { result ->
                result.onSuccess {
                    loadExercises(workoutId)
                    if (!_availableExerciseNames.value.contains(name)) {
                        _availableExerciseNames.value = (_availableExerciseNames.value + name).sorted()
                    }
                }
            }
        }
    }

    fun updateExercise(exercise: Exercise) {
        val workoutId = _selectedWorkout.value?.id ?: return
        userId?.let { uid ->
            repository.updateExercise(uid, workoutId, exercise) { result ->
                result.onSuccess {
                    loadExercises(workoutId)
                    if (!_availableExerciseNames.value.contains(exercise.name)) {
                        _availableExerciseNames.value = (_availableExerciseNames.value + exercise.name).sorted()
                    }
                }
            }
        }
    }

    fun deleteExercise(exerciseId: String) {
        val workoutId = _selectedWorkout.value?.id ?: return
        userId?.let { uid ->
            repository.deleteExercise(uid, workoutId, exerciseId) { result ->
                result.onSuccess { loadExercises(workoutId) }
            }
        }
    }
}