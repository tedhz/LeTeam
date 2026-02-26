package com.leteam.locked.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.leteam.locked.workout.Exercise
import com.leteam.locked.workout.Workout
import com.leteam.locked.workout.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkoutsFeedUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val feedItems: List<WorkoutFeedItem> = emptyList()
)

data class WorkoutFeedItem(
    val workout: Workout,
    val userDisplayName: String,
    val exercises: List<Exercise>
)

class WorkoutsFeedViewModel(
    private val repo: WorkoutRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutsFeedUiState())
    val uiState: StateFlow<WorkoutsFeedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = WorkoutsFeedUiState(isLoading = true)

        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            _uiState.value = WorkoutsFeedUiState(
                isLoading = false,
                errorMessage = "Not signed in."
            )
            return
        }

        repo.getWorkoutsFeedForUsers(
            currentUserId = myUid,
            followedUserIds = emptyList(), //TEMPORARY
            perUserLimit = 10
        ) { feedResult ->
            feedResult.fold(
                onSuccess = { workouts ->
                    if (workouts.isEmpty()) {
                        _uiState.value = WorkoutsFeedUiState(
                            isLoading = false,
                            feedItems = emptyList()
                        )
                        return@getWorkoutsFeedForUsers
                    }

                    val items = mutableListOf<WorkoutFeedItem>()
                    var remaining = workouts.size
                    var failedOnce: String? = null

                    workouts.forEach { w ->
                        repo.getUserDisplayName(w.userId) { nameResult ->
                            val displayName = nameResult.getOrNull() ?: w.userId

                            repo.getExercisesForWorkout(w.userId, w.id) { exResult ->
                                val exercises = exResult.getOrNull().orEmpty()
                                if (exResult.isFailure && failedOnce == null) {
                                    failedOnce = exResult.exceptionOrNull()?.message
                                }

                                items.add(
                                    WorkoutFeedItem(
                                        workout = w,
                                        userDisplayName = displayName,
                                        exercises = exercises
                                    )
                                )

                                remaining -= 1
                                if (remaining == 0) {
                                    val sorted = items.sortedByDescending { it.workout.workoutDate }
                                    _uiState.value = WorkoutsFeedUiState(
                                        isLoading = false,
                                        errorMessage = failedOnce,
                                        feedItems = sorted
                                    )
                                }
                            }
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.value = WorkoutsFeedUiState(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load workouts."
                    )
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = FirebaseFirestore.getInstance()
                val repo = WorkoutRepository(db)
                val auth = FirebaseAuth.getInstance()
                return WorkoutsFeedViewModel(repo, auth) as T
            }
        }
    }
}