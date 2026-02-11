package com.leteam.locked.workout

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class WorkoutRepositoryTest {

    private val mockFirestore: FirebaseFirestore = mockk()
    private val mockBatch: WriteBatch = mockk(relaxed = true)
    private val mockUsersCollection: CollectionReference = mockk()
    private val mockUserDoc: DocumentReference = mockk()
    private val mockWorkoutsCollection: CollectionReference = mockk()
    private val mockWorkoutDoc: DocumentReference = mockk()
    private val mockExercisesCollection: CollectionReference = mockk()
    private val mockExerciseDoc: DocumentReference = mockk()

    private val mockTaskVoid: Task<Void> = mockk()
    private val mockTaskQuery: Task<QuerySnapshot> = mockk()
    private val mockQuery: Query = mockk()
    private val mockQuerySnapshot: QuerySnapshot = mockk()
    private val mockDocSnapshot: QueryDocumentSnapshot = mockk()

    private lateinit var repository: WorkoutRepository

    private val testUserId = "user_123"
    private val testWorkoutId = "workout_abc"

    @Before
    fun setUp() {
        repository = WorkoutRepository(mockFirestore)

        every { mockFirestore.collection("users") } returns mockUsersCollection
        every { mockUsersCollection.document(any()) } returns mockUserDoc
        every { mockUserDoc.collection("workouts") } returns mockWorkoutsCollection

        every { mockWorkoutsCollection.document() } returns mockWorkoutDoc
        every { mockWorkoutsCollection.document(any()) } returns mockWorkoutDoc
        every { mockWorkoutDoc.id } returns testWorkoutId

        every { mockWorkoutDoc.collection("exercises") } returns mockExercisesCollection
        every { mockExercisesCollection.document() } returns mockExerciseDoc

        every { mockFirestore.batch() } returns mockBatch

        every { mockBatch.commit() } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockTaskVoid
        }
        every { mockTaskVoid.addOnFailureListener(any()) } returns mockTaskVoid
    }

    @Test
    fun `createWorkout allows empty list (Start Workout scenario)`() {
        var resultCaptured: Result<String>? = null

        repository.createWorkout(testUserId, Date()) { result ->
            resultCaptured = result
        }

        assertTrue(resultCaptured!!.isSuccess)
        assertEquals(testWorkoutId, resultCaptured!!.getOrNull())

        verify { mockBatch.set(mockWorkoutDoc, any()) }

        verify(exactly = 0) { mockBatch.set(mockExerciseDoc, any()) }

        verify { mockBatch.commit() }
    }

    @Test
    fun `createWorkout logs workout and exercises successfully when list provided`() {
        val exercises = listOf(
            Exercise(name = "Bench Press", numberOfSets = 3, repsPerSet = 10, weightAmount = 135.0),
            Exercise(name = "Squat", numberOfSets = 3, repsPerSet = 8, weightAmount = 225.0)
        )

        var resultCaptured: Result<String>? = null

        repository.createWorkout(testUserId, Date(), exercises) { result ->
            resultCaptured = result
        }

        assertTrue(resultCaptured!!.isSuccess)

        verify { mockBatch.set(mockWorkoutDoc, any()) }

        verify(exactly = 2) { mockBatch.set(mockExerciseDoc, any()) }
    }

    @Test
    fun `addExercisesToWorkout adds exercises to existing document`() {
        val newExercises = listOf(
            Exercise(name = "Deadlift", numberOfSets = 1, repsPerSet = 5, weightAmount = 315.0)
        )

        var resultCaptured: Result<Unit>? = null

        repository.addExercisesToWorkout(testUserId, testWorkoutId, newExercises) {
            resultCaptured = it
        }

        assertTrue(resultCaptured!!.isSuccess)

        verify { mockWorkoutsCollection.document(testWorkoutId) }

        verify { mockBatch.set(mockExerciseDoc, any()) }

        verify { mockBatch.commit() }
    }

    @Test
    fun `addExercisesToWorkout returns failure when list is empty`() {
        var resultCaptured: Result<Unit>? = null

        repository.addExercisesToWorkout(testUserId, testWorkoutId, emptyList()) {
            resultCaptured = it
        }

        assertTrue(resultCaptured!!.isFailure)

        verify(exactly = 0) { mockBatch.commit() }
    }

    @Test
    fun `getWorkouts parses data correctly`() {
        every { mockWorkoutsCollection.orderBy("workoutDate", Query.Direction.DESCENDING) } returns mockQuery
        every { mockQuery.get() } returns mockTaskQuery

        every { mockDocSnapshot.id } returns testWorkoutId
        every { mockDocSnapshot.getTimestamp("workoutDate") } returns Timestamp.now()
        every { mockDocSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
        every { mockQuerySnapshot.documents } returns listOf(mockDocSnapshot)

        every { mockTaskQuery.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockQuerySnapshot)
            mockTaskQuery
        }
        every { mockTaskQuery.addOnFailureListener(any()) } returns mockTaskQuery

        var resultList: List<Workout>? = null

        repository.getWorkouts(testUserId) { result ->
            resultList = result.getOrNull()
        }

        assertTrue(resultList != null)
        assertEquals(1, resultList!!.size)
        assertEquals(testWorkoutId, resultList!![0].id)
    }
}