package com.leteam.locked.ui.screens.insights

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.workout.Exercise
import com.leteam.locked.workout.Workout
import com.leteam.locked.workout.WorkoutRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class InsightsViewModelTest {

    private val testUserId = "user_123"
    private val mockWorkoutRepo: WorkoutRepository = mockk(relaxed = true)
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    private lateinit var viewModel: InsightsViewModel

    @Before
    fun setUp() {
        mockkObject(FirebaseProvider)
        every { FirebaseProvider.auth } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUserId
        viewModel = InsightsViewModel(mockWorkoutRepo)
    }

    @After
    fun tearDown() {
        unmockkObject(FirebaseProvider)
    }

    @Test
    fun `load when user is null sets Error state`() {
        every { mockAuth.currentUser } returns null
        viewModel = InsightsViewModel(mockWorkoutRepo)

        viewModel.load()

        val state = viewModel.uiState.value
        assertTrue(state is InsightsUiState.Error)
        assertEquals("User not logged in", (state as InsightsUiState.Error).message)
    }

    @Test
    fun `load when no workouts sets Empty state`() {
        every { mockWorkoutRepo.getWorkouts(testUserId, any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        viewModel.load()

        val state = viewModel.uiState.value
        assertTrue(state is InsightsUiState.Empty)
        assertEquals("No workouts yet", (state as InsightsUiState.Empty).message)
    }

    @Test
    fun `load with workouts and exercises sets Loaded state`() {
        val workout = Workout("w1", testUserId, Date(), Date())
        val exercise = Exercise("e1", "Squat", 3, 10, 225.0)
        every { mockWorkoutRepo.getWorkouts(testUserId, any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(listOf(workout)))
        }
        every { mockWorkoutRepo.getExercisesForWorkout(testUserId, "w1", any()) } answers {
            lastArg<(Result<List<Exercise>>) -> Unit>().invoke(Result.success(listOf(exercise)))
        }

        viewModel.load()

        val state = viewModel.uiState.value
        assertTrue(state is InsightsUiState.Loaded)
        val loaded = state as InsightsUiState.Loaded
        assertEquals(listOf("Squat"), loaded.exercises)
        assertEquals("Squat", loaded.selectedExercise)
        assertEquals("Squat", loaded.insights.exerciseName)
        assertEquals(225.0, loaded.insights.prWeight, 1e-6)
    }

    @Test
    fun `setExercise updates selected exercise and publishes`() {
        val workout = Workout("w1", testUserId, Date(), Date())
        val ex1 = Exercise("e1", "Squat", 3, 10, 225.0)
        val ex2 = Exercise("e2", "Bench", 3, 10, 135.0)
        every { mockWorkoutRepo.getWorkouts(testUserId, any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(listOf(workout)))
        }
        every { mockWorkoutRepo.getExercisesForWorkout(testUserId, "w1", any()) } answers {
            lastArg<(Result<List<Exercise>>) -> Unit>().invoke(Result.success(listOf(ex1, ex2)))
        }
        viewModel.load()

        viewModel.setExercise("Bench")

        val state = viewModel.uiState.value as InsightsUiState.Loaded
        assertEquals("Bench", state.selectedExercise)
        assertEquals("Bench", state.insights.exerciseName)
    }

    @Test
    fun `setRange updates range and publishes`() {
        val workout = Workout("w1", testUserId, Date(), Date())
        val exercise = Exercise("e1", "Squat", 3, 10, 225.0)
        every { mockWorkoutRepo.getWorkouts(testUserId, any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(listOf(workout)))
        }
        every { mockWorkoutRepo.getExercisesForWorkout(testUserId, "w1", any()) } answers {
            lastArg<(Result<List<Exercise>>) -> Unit>().invoke(Result.success(listOf(exercise)))
        }
        viewModel.load()

        viewModel.setRange(InsightsRange.D7)

        val state = viewModel.uiState.value as InsightsUiState.Loaded
        assertEquals(InsightsRange.D7, state.range)
    }
}
