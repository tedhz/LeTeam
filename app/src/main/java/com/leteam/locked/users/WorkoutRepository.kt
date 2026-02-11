package com.leteam.locked.workout

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

data class Exercise(
    val id: String = "",
    val name: String,
    val numberOfSets: Int,
    val repsPerSet: Int,
    val weightAmount: Double
)

data class Workout(
    val id: String,
    val userId: String,
    val workoutDate: Date,
    val createdAt: Date
)

class WorkoutRepository(
    private val db: FirebaseFirestore
) {

    private fun workoutsCollection(userId: String) =
        db.collection("users").document(userId).collection("workouts")

    fun createWorkout(
        userId: String,
        workoutDate: Date,
        exercises: List<Exercise> = emptyList(),
        onResult: (Result<String>) -> Unit
    ) {

        val batch = db.batch()

        val newWorkoutRef = workoutsCollection(userId).document()
        val workoutId = newWorkoutRef.id

        val workoutPayload = hashMapOf(
            "workoutDate" to Timestamp(workoutDate),
            "createdAt" to Timestamp.now()
        )
        batch.set(newWorkoutRef, workoutPayload)

        if (exercises.isNotEmpty()) {
            val exercisesRef = newWorkoutRef.collection("exercises")
            exercises.forEach { exercise ->
                val newExerciseRef = exercisesRef.document()
                val exercisePayload = hashMapOf(
                    "exerciseName" to exercise.name,
                    "numberOfSets" to exercise.numberOfSets,
                    "repsPerSet" to exercise.repsPerSet,
                    "weightAmount" to exercise.weightAmount,
                    "createdAt" to Timestamp.now()
                )
                batch.set(newExerciseRef, exercisePayload)
            }
        }

        batch.commit()
            .addOnSuccessListener { onResult(Result.success(workoutId)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun addExercisesToWorkout(
        userId: String,
        workoutId: String,
        exercises: List<Exercise>,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (exercises.isEmpty()) {
            onResult(Result.failure(IllegalArgumentException("No exercises to add")))
            return
        }

        val batch = db.batch()
        val exercisesRef = workoutsCollection(userId).document(workoutId).collection("exercises")

        exercises.forEach { exercise ->
            val newExerciseRef = exercisesRef.document()
            val exercisePayload = hashMapOf(
                "exerciseName" to exercise.name,
                "numberOfSets" to exercise.numberOfSets,
                "repsPerSet" to exercise.repsPerSet,
                "weightAmount" to exercise.weightAmount,
                "createdAt" to Timestamp.now()
            )
            batch.set(newExerciseRef, exercisePayload)
        }

        batch.commit()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getWorkouts(
        userId: String,
        onResult: (Result<List<Workout>>) -> Unit
    ) {
        workoutsCollection(userId)
            .orderBy("workoutDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val workouts = snapshot.documents.map { doc ->
                    val workoutDate = doc.getTimestamp("workoutDate")?.toDate() ?: Date()
                    val createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()

                    Workout(
                        id = doc.id,
                        userId = userId,
                        workoutDate = workoutDate,
                        createdAt = createdAt
                    )
                }
                onResult(Result.success(workouts))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getExercisesForWorkout(
        userId: String,
        workoutId: String,
        onResult: (Result<List<Exercise>>) -> Unit
    ) {
        workoutsCollection(userId).document(workoutId).collection("exercises")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val exercises = snapshot.documents.map { doc ->
                    Exercise(
                        id = doc.id,
                        name = doc.getString("exerciseName") ?: "Unknown",
                        numberOfSets = doc.getLong("numberOfSets")?.toInt() ?: 0,
                        repsPerSet = doc.getLong("repsPerSet")?.toInt() ?: 0,
                        weightAmount = doc.getDouble("weightAmount") ?: 0.0
                    )
                }
                onResult(Result.success(exercises))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }
}