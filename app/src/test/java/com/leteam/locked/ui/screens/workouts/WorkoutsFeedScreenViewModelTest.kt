package com.leteam.locked.ui.screens.workouts

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.workout.Exercise
import com.leteam.locked.workout.Workout
import com.leteam.locked.workout.WorkoutRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class WorkoutsFeedScreenViewModelTest {

    @Test
    fun `init sets Not signed in when currentUser is null`() {
        val repo: WorkoutRepository = mockk(relaxed = true)
        val auth: FirebaseAuth = mockk()
        every { auth.currentUser } returns null

        val vm = WorkoutsFeedViewModel(repo, auth)

        val state = vm.uiState.value
        assertTrue(!state.isLoading)
        assertEquals("Not signed in.", state.errorMessage)
        assertTrue(state.feedItems.isEmpty())

        verify(exactly = 0) { repo.getWorkoutsFeedForUsers(any(), any(), any(), any()) }
    }

    @Test
    fun `init sets empty feedItems when workouts list is empty`() {
        val repo: WorkoutRepository = mockk(relaxed = true)
        val auth: FirebaseAuth = mockk()
        val user: FirebaseUser = mockk()

        every { user.uid } returns "u1"
        every { auth.currentUser } returns user

        every {
            repo.getWorkoutsFeedForUsers(
                currentUserId = "u1",
                followedUserIds = emptyList(),
                perUserLimit = 10,
                onResult = any()
            )
        } answers {
            val cb = lastArg<(Result<List<Workout>>) -> Unit>()
            cb(Result.success(emptyList()))
        }

        val vm = WorkoutsFeedViewModel(repo, auth)

        val state = vm.uiState.value
        assertTrue(!state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.feedItems.isEmpty())
    }

    @Test
    fun `init builds sorted feed items with display name and exercises`() {
        val repo: WorkoutRepository = mockk(relaxed = true)
        val auth: FirebaseAuth = mockk()
        val user: FirebaseUser = mockk()

        every { user.uid } returns "me"
        every { auth.currentUser } returns user

        val newerDate = Date(2000L)
        val olderDate = Date(1000L)

        val workouts = listOf(
            Workout(id = "w_old", userId = "u2", workoutDate = olderDate, createdAt = olderDate),
            Workout(id = "w_new", userId = "u1", workoutDate = newerDate, createdAt = newerDate)
        )

        every {
            repo.getWorkoutsFeedForUsers(
                currentUserId = "me",
                followedUserIds = emptyList(),
                perUserLimit = 10,
                onResult = any()
            )
        } answers {
            val cb = lastArg<(Result<List<Workout>>) -> Unit>()
            cb(Result.success(workouts))
        }

        every { repo.getUserDisplayName("u1", any()) } answers {
            val cb = secondArg<(Result<String>) -> Unit>()
            cb(Result.success("Alice"))
        }
        every { repo.getUserDisplayName("u2", any()) } answers {
            val cb = secondArg<(Result<String>) -> Unit>()
            cb(Result.success("Bob"))
        }

        every { repo.getExercisesForWorkout("u1", "w_new", any()) } answers {
            val cb = thirdArg<(Result<List<Exercise>>) -> Unit>()
            cb(
                Result.success(
                    listOf(
                        Exercise(
                            id = "e1",
                            name = "Bench Press",
                            numberOfSets = 3,
                            repsPerSet = 10,
                            weightAmount = 135.0
                        )
                    )
                )
            )
        }
        every { repo.getExercisesForWorkout("u2", "w_old", any()) } answers {
            val cb = thirdArg<(Result<List<Exercise>>) -> Unit>()
            cb(Result.success(emptyList()))
        }

        val vm = WorkoutsFeedViewModel(repo, auth)

        val state = vm.uiState.value
        assertTrue(!state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.feedItems.size)

        assertEquals("w_new", state.feedItems[0].workout.id)
        assertEquals("Alice", state.feedItems[0].userDisplayName)
        assertEquals(1, state.feedItems[0].exercises.size)

        assertEquals("w_old", state.feedItems[1].workout.id)
        assertEquals("Bob", state.feedItems[1].userDisplayName)
        assertEquals(0, state.feedItems[1].exercises.size)

        verify(exactly = 1) {
            repo.getWorkoutsFeedForUsers(
                currentUserId = "me",
                followedUserIds = emptyList(),
                perUserLimit = 10,
                onResult = any()
            )
        }
    }

    @Test
    fun `init sets errorMessage when feed load fails`() {
        val repo: WorkoutRepository = mockk(relaxed = true)
        val auth: FirebaseAuth = mockk()
        val user: FirebaseUser = mockk()

        every { user.uid } returns "me"
        every { auth.currentUser } returns user

        every {
            repo.getWorkoutsFeedForUsers(
                currentUserId = "me",
                followedUserIds = emptyList(),
                perUserLimit = 10,
                onResult = any()
            )
        } answers {
            val cb = lastArg<(Result<List<Workout>>) -> Unit>()
            cb(Result.failure(Exception("boom")))
        }

        val vm = WorkoutsFeedViewModel(repo, auth)

        val state = vm.uiState.value
        assertTrue(!state.isLoading)
        assertEquals("boom", state.errorMessage)
        assertTrue(state.feedItems.isEmpty())
    }

    @Test
    fun `init keeps going when exercises fetch fails and surfaces message`() {
        val repo: WorkoutRepository = mockk(relaxed = true)
        val auth: FirebaseAuth = mockk()
        val user: FirebaseUser = mockk()

        every { user.uid } returns "me"
        every { auth.currentUser } returns user

        val workouts = listOf(
            Workout(id = "w1", userId = "u1", workoutDate = Date(2000L), createdAt = Date(2000L))
        )

        every {
            repo.getWorkoutsFeedForUsers(
                currentUserId = "me",
                followedUserIds = emptyList(),
                perUserLimit = 10,
                onResult = any()
            )
        } answers {
            val cb = lastArg<(Result<List<Workout>>) -> Unit>()
            cb(Result.success(workouts))
        }

        every { repo.getUserDisplayName("u1", any()) } answers {
            val cb = secondArg<(Result<String>) -> Unit>()
            cb(Result.success("Alice"))
        }

        every { repo.getExercisesForWorkout("u1", "w1", any()) } answers {
            val cb = thirdArg<(Result<List<Exercise>>) -> Unit>()
            cb(Result.failure(Exception("exercise fail")))
        }

        val vm = WorkoutsFeedViewModel(repo, auth)

        val state = vm.uiState.value
        assertTrue(!state.isLoading)
        assertEquals("exercise fail", state.errorMessage)
        assertEquals(1, state.feedItems.size)
        assertEquals("w1", state.feedItems[0].workout.id)
        assertEquals("Alice", state.feedItems[0].userDisplayName)
        assertTrue(state.feedItems[0].exercises.isEmpty())
    }
}