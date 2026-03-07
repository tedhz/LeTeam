package com.leteam.locked.ui.screens.workouts

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.workout.Exercise
import com.leteam.locked.workout.Workout
import com.leteam.locked.workout.WorkoutRepository
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class MyWorkoutsViewModelTest {

    private val mockRepo: WorkoutRepository = mockk(relaxed = true)
    private val mockAuth: FirebaseAuth = mockk()
    private val mockUser: FirebaseUser = mockk()

    private lateinit var viewModel: MyWorkoutsViewModel

    private val testUserId = "user_123"

    @Before
    fun setUp() {
        mockkObject(FirebaseProvider)
        every { FirebaseProvider.auth } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUserId

        every { mockRepo.getWorkouts(any(), any()) } answers {
            val callback = lastArg<(Result<List<Workout>>) -> Unit>()
            callback(Result.success(emptyList()))
        }

        viewModel = MyWorkoutsViewModel(mockRepo)
    }

    @After
    fun tearDown() {
        unmockkObject(FirebaseProvider)
    }

    @Test
    fun `loadWorkouts fetches workouts and aggregates available exercise names`() {
        val workout1 = Workout("w1", testUserId, Date(), Date())
        val workout2 = Workout("w2", testUserId, Date(), Date())
        val workoutsList = listOf(workout1, workout2)

        val exercisesW1 = listOf(Exercise("e1", "Squat", 3, 10, 225.0))
        val exercisesW2 = listOf(
            Exercise("e2", "Bench Press", 3, 10, 135.0),
            Exercise("e3", "Squat", 3, 5, 245.0)
        )

        val workoutCallbackSlot = slot<(Result<List<Workout>>) -> Unit>()
        every { mockRepo.getWorkouts(testUserId, capture(workoutCallbackSlot)) } answers {
            workoutCallbackSlot.captured.invoke(Result.success(workoutsList))
        }

        every { mockRepo.getExercisesForWorkout(testUserId, "w1", any()) } answers {
            val callback = lastArg<(Result<List<Exercise>>) -> Unit>()
            callback(Result.success(exercisesW1))
        }

        every { mockRepo.getExercisesForWorkout(testUserId, "w2", any()) } answers {
            val callback = lastArg<(Result<List<Exercise>>) -> Unit>()
            callback(Result.success(exercisesW2))
        }

        viewModel.loadWorkouts()

        assertEquals(workoutsList, viewModel.workouts.value)

        assertEquals(listOf("Bench Press", "Squat"), viewModel.availableExerciseNames.value)
    }

    @Test
    fun `createWorkout calls repository and reloads workouts upon success`() {
        val createCallbackSlot = slot<(Result<String>) -> Unit>()
        every { mockRepo.createWorkout(testUserId, any(), any(), capture(createCallbackSlot)) } answers {
            createCallbackSlot.captured.invoke(Result.success("new_w_id"))
        }

        viewModel.createWorkout()

        verify { mockRepo.createWorkout(testUserId, any(), emptyList(), any()) }

        verify(exactly = 2) { mockRepo.getWorkouts(testUserId, any()) }
    }

    @Test
    fun `selectWorkout sets selectedWorkout and loads its associated exercises`() {
        val workout = Workout("w1", testUserId, Date(), Date())
        val exercisesList = listOf(Exercise("e1", "Deadlift", 3, 5, 315.0))

        every { mockRepo.getExercisesForWorkout(testUserId, "w1", any()) } answers {
            val callback = lastArg<(Result<List<Exercise>>) -> Unit>()
            callback(Result.success(exercisesList))
        }

        viewModel.selectWorkout(workout)

        assertEquals(workout, viewModel.selectedWorkout.value)
        assertEquals(exercisesList, viewModel.exercises.value)
    }

    @Test
    fun `clearSelectedWorkout nullifies selectedWorkout and clears exercises list`() {
        val workout = Workout("w1", testUserId, Date(), Date())
        viewModel.selectWorkout(workout)

        viewModel.clearSelectedWorkout()

        assertNull(viewModel.selectedWorkout.value)
        assertEquals(emptyList<Exercise>(), viewModel.exercises.value)
    }

    @Test
    fun `addExercise calls repository, reloads exercises, and adds name to available names list`() {
        val workout = Workout("w1", testUserId, Date(), Date())
        viewModel.selectWorkout(workout)

        val newExerciseName = "Pull Up"

        val addExerciseSlot = slot<(Result<Unit>) -> Unit>()
        every { mockRepo.addExercisesToWorkout(testUserId, "w1", any(), capture(addExerciseSlot)) } answers {
            addExerciseSlot.captured.invoke(Result.success(Unit))
        }

        viewModel.addExercise(newExerciseName, 3, 10, 0.0)

        verify { mockRepo.addExercisesToWorkout(testUserId, "w1", match { it[0].name == newExerciseName }, any()) }

        verify(exactly = 2) { mockRepo.getExercisesForWorkout(testUserId, "w1", any()) }

        assertTrue(viewModel.availableExerciseNames.value.contains(newExerciseName))
    }

    @Test
    fun `updateExercise calls repository, reloads exercises, and updates available names`() {
        val workout = Workout("w1", testUserId, Date(), Date())
        viewModel.selectWorkout(workout)

        val exercise = Exercise("e1", "Push Up", 3, 20, 0.0)

        val updateExerciseSlot = slot<(Result<Unit>) -> Unit>()
        every { mockRepo.updateExercise(testUserId, "w1", exercise, capture(updateExerciseSlot)) } answers {
            updateExerciseSlot.captured.invoke(Result.success(Unit))
        }

        viewModel.updateExercise(exercise)

        verify { mockRepo.updateExercise(testUserId, "w1", exercise, any()) }
        verify(exactly = 2) { mockRepo.getExercisesForWorkout(testUserId, "w1", any()) }

        assertTrue(viewModel.availableExerciseNames.value.contains("Push Up"))
    }

    @Test
    fun `deleteExercise calls repository and reloads exercises upon success`() {
        val workout = Workout("w1", testUserId, Date(), Date())
        viewModel.selectWorkout(workout)

        val deleteExerciseSlot = slot<(Result<Unit>) -> Unit>()
        every { mockRepo.deleteExercise(testUserId, "w1", "e1", capture(deleteExerciseSlot)) } answers {
            deleteExerciseSlot.captured.invoke(Result.success(Unit))
        }

        viewModel.deleteExercise("e1")

        verify { mockRepo.deleteExercise(testUserId, "w1", "e1", any()) }

        verify(exactly = 2) { mockRepo.getExercisesForWorkout(testUserId, "w1", any()) }
    }
}