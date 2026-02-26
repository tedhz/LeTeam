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

    private val mockTaskDoc: Task<DocumentSnapshot> = mockk()
    private val mockUserDocSnapshot: DocumentSnapshot = mockk()

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

    @Test
    fun `getWorkoutsFeedForUsers returns merged sorted list`() {
        val currentUser = "me"
        val followedUser = "friend"

        val mockUserDocMe: DocumentReference = mockk()
        val mockUserDocFriend: DocumentReference = mockk()
        val mockWorkoutsMe: CollectionReference = mockk()
        val mockWorkoutsFriend: CollectionReference = mockk()

        val mockQueryMe: Query = mockk()
        val mockQueryFriend: Query = mockk()
        val mockQueryMeLimited: Query = mockk()
        val mockQueryFriendLimited: Query = mockk()

        val taskMe: Task<QuerySnapshot> = mockk()
        val taskFriend: Task<QuerySnapshot> = mockk()

        val snapMe: QuerySnapshot = mockk()
        val snapFriend: QuerySnapshot = mockk()

        val docMe: QueryDocumentSnapshot = mockk()
        val docFriend: QueryDocumentSnapshot = mockk()

        every { mockUsersCollection.document(currentUser) } returns mockUserDocMe
        every { mockUsersCollection.document(followedUser) } returns mockUserDocFriend

        every { mockUserDocMe.collection("workouts") } returns mockWorkoutsMe
        every { mockUserDocFriend.collection("workouts") } returns mockWorkoutsFriend

        every { mockWorkoutsMe.orderBy("workoutDate", Query.Direction.DESCENDING) } returns mockQueryMe
        every { mockWorkoutsFriend.orderBy("workoutDate", Query.Direction.DESCENDING) } returns mockQueryFriend

        every { mockQueryMe.limit(any()) } returns mockQueryMeLimited
        every { mockQueryFriend.limit(any()) } returns mockQueryFriendLimited

        every { mockQueryMeLimited.get() } returns taskMe
        every { mockQueryFriendLimited.get() } returns taskFriend

        val newer = Date(2000L)
        val older = Date(1000L)

        every { docMe.id } returns "w_me"
        every { docMe.getTimestamp("workoutDate") } returns Timestamp(newer)
        every { docMe.getTimestamp("createdAt") } returns Timestamp(newer)

        every { docFriend.id } returns "w_friend"
        every { docFriend.getTimestamp("workoutDate") } returns Timestamp(older)
        every { docFriend.getTimestamp("createdAt") } returns Timestamp(older)

        every { snapMe.documents } returns listOf(docMe)
        every { snapFriend.documents } returns listOf(docFriend)

        every { taskMe.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(snapMe)
            taskMe
        }
        every { taskMe.addOnFailureListener(any()) } returns taskMe

        every { taskFriend.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(snapFriend)
            taskFriend
        }
        every { taskFriend.addOnFailureListener(any()) } returns taskFriend

        var captured: Result<List<Workout>>? = null

        repository.getWorkoutsFeedForUsers(
            currentUserId = currentUser,
            followedUserIds = listOf(followedUser),
            perUserLimit = 10
        ) { result ->
            captured = result
        }

        assertTrue(captured!!.isSuccess)
        val list = captured!!.getOrNull()!!
        assertEquals(2, list.size)
        assertEquals("w_me", list[0].id)
        assertEquals(currentUser, list[0].userId)
        assertEquals("w_friend", list[1].id)
        assertEquals(followedUser, list[1].userId)
    }

    @Test
    fun `getWorkoutsFeedForUsers returns failure when all queries fail and no data`() {
        val currentUser = "me"

        val mockUserDocMe: DocumentReference = mockk()
        val mockWorkoutsMe: CollectionReference = mockk()
        val mockQueryMe: Query = mockk()
        val mockQueryMeLimited: Query = mockk()
        val taskMe: Task<QuerySnapshot> = mockk()

        val exception = Exception("fail")

        every { mockUsersCollection.document(currentUser) } returns mockUserDocMe
        every { mockUserDocMe.collection("workouts") } returns mockWorkoutsMe
        every { mockWorkoutsMe.orderBy("workoutDate", Query.Direction.DESCENDING) } returns mockQueryMe
        every { mockQueryMe.limit(any()) } returns mockQueryMeLimited
        every { mockQueryMeLimited.get() } returns taskMe

        every { taskMe.addOnSuccessListener(any()) } returns taskMe
        every { taskMe.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            taskMe
        }

        var captured: Result<List<Workout>>? = null

        repository.getWorkoutsFeedForUsers(
            currentUserId = currentUser,
            followedUserIds = emptyList(),
            perUserLimit = 10
        ) { result ->
            captured = result
        }

        assertTrue(captured!!.isFailure)
        assertEquals("fail", captured!!.exceptionOrNull()?.message)
    }

    @Test
    fun `getUserDisplayName returns displayName when present`() {
        every { mockUsersCollection.document("u1") } returns mockUserDoc
        every { mockUserDoc.get() } returns mockTaskDoc

        every { mockUserDocSnapshot.getString("displayName") } returns "Cliff"
        every { mockUserDocSnapshot.getString("username") } returns null
        every { mockUserDocSnapshot.getString("name") } returns null

        every { mockTaskDoc.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockUserDocSnapshot)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc

        var captured: Result<String>? = null

        repository.getUserDisplayName("u1") { result ->
            captured = result
        }

        assertTrue(captured!!.isSuccess)
        assertEquals("Cliff", captured!!.getOrNull())
    }

    @Test
    fun `getUserDisplayName falls back to userId when no fields present`() {
        every { mockUsersCollection.document("u2") } returns mockUserDoc
        every { mockUserDoc.get() } returns mockTaskDoc

        every { mockUserDocSnapshot.getString("displayName") } returns null
        every { mockUserDocSnapshot.getString("username") } returns null
        every { mockUserDocSnapshot.getString("name") } returns null

        every { mockTaskDoc.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockUserDocSnapshot)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc

        var captured: Result<String>? = null

        repository.getUserDisplayName("u2") { result ->
            captured = result
        }

        assertTrue(captured!!.isSuccess)
        assertEquals("u2", captured!!.getOrNull())
    }

    @Test
    fun `updateExercise successfully updates document`() {
        every { mockExercisesCollection.document(any()) } returns mockExerciseDoc
        every { mockExerciseDoc.update(any<Map<String, Any>>()) } returns mockTaskVoid

        val exerciseToUpdate = Exercise(
            id = "exercise_123",
            name = "Incline Dumbbell Press",
            numberOfSets = 4,
            repsPerSet = 10,
            weightAmount = 65.0
        )

        var resultCaptured: Result<Unit>? = null

        repository.updateExercise(testUserId, testWorkoutId, exerciseToUpdate) {
            resultCaptured = it
        }

        assertTrue(resultCaptured!!.isSuccess)

        val expectedPayload = mapOf(
            "exerciseName" to exerciseToUpdate.name,
            "numberOfSets" to exerciseToUpdate.numberOfSets,
            "repsPerSet" to exerciseToUpdate.repsPerSet,
            "weightAmount" to exerciseToUpdate.weightAmount
        )
        verify { mockExerciseDoc.update(expectedPayload) }
    }

    @Test
    fun `updateExercise returns failure when update fails`() {
        every { mockExercisesCollection.document(any()) } returns mockExerciseDoc

        val exception = Exception("Network error")
        val mockFailingTask: Task<Void> = mockk()

        every { mockExerciseDoc.update(any<Map<String, Any>>()) } returns mockFailingTask
        every { mockFailingTask.addOnSuccessListener(any()) } returns mockFailingTask
        every { mockFailingTask.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockFailingTask
        }

        val exerciseToUpdate = Exercise(
            id = "exercise_123",
            name = "Incline Dumbbell Press",
            numberOfSets = 4,
            repsPerSet = 10,
            weightAmount = 65.0
        )

        var resultCaptured: Result<Unit>? = null

        repository.updateExercise(testUserId, testWorkoutId, exerciseToUpdate) {
            resultCaptured = it
        }

        assertTrue(resultCaptured!!.isFailure)
        assertEquals("Network error", resultCaptured!!.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteExercise successfully deletes document`() {
        every { mockExercisesCollection.document(any()) } returns mockExerciseDoc
        every { mockExerciseDoc.delete() } returns mockTaskVoid

        val testExerciseId = "exercise_123"
        var resultCaptured: Result<Unit>? = null

        repository.deleteExercise(testUserId, testWorkoutId, testExerciseId) {
            resultCaptured = it
        }

        assertTrue(resultCaptured!!.isSuccess)

        verify { mockWorkoutsCollection.document(testWorkoutId) }
        verify { mockExercisesCollection.document(testExerciseId) }
        verify { mockExerciseDoc.delete() }
    }

    @Test
    fun `deleteExercise returns failure when delete fails`() {
        every { mockExercisesCollection.document(any()) } returns mockExerciseDoc

        val exception = Exception("Permission denied")
        val mockFailingTask: Task<Void> = mockk()

        every { mockExerciseDoc.delete() } returns mockFailingTask
        every { mockFailingTask.addOnSuccessListener(any()) } returns mockFailingTask
        every { mockFailingTask.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockFailingTask
        }

        val testExerciseId = "exercise_123"
        var resultCaptured: Result<Unit>? = null

        repository.deleteExercise(testUserId, testWorkoutId, testExerciseId) {
            resultCaptured = it
        }

        assertTrue(resultCaptured!!.isFailure)
        assertEquals("Permission denied", resultCaptured!!.exceptionOrNull()?.message)
    }
}