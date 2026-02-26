package com.leteam.locked.ui.screens.workouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leteam.locked.workout.Exercise
import com.leteam.locked.workout.Workout
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWorkoutsScreen(
    viewModel: MyWorkoutsViewModel = viewModel()
) {
    val workouts by viewModel.workouts.collectAsState()
    val selectedWorkout by viewModel.selectedWorkout.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val availableExerciseNames by viewModel.availableExerciseNames.collectAsState()

    var showExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }

    if (selectedWorkout == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("My Workouts") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.createWorkout() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Workout")
                }
            }
        ) { padding ->
            if (workouts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No workouts found. Click + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(workouts) { workout ->
                        WorkoutItem(workout) { viewModel.selectWorkout(workout) }
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Workout Details") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelectedWorkout() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    exerciseToEdit = null
                    showExerciseDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
            }
        ) { padding ->
            if (exercises.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No exercises added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(exercises) { exercise ->
                        ExerciseItem(
                            exercise = exercise,
                            onEdit = {
                                exerciseToEdit = exercise
                                showExerciseDialog = true
                            },
                            onDelete = { viewModel.deleteExercise(exercise.id) }
                        )
                    }
                }
            }
        }
    }

    if (showExerciseDialog) {
        ExerciseDialog(
            initialExercise = exerciseToEdit,
            availableNames = availableExerciseNames,
            onDismiss = { showExerciseDialog = false },
            onSave = { name, sets, reps, weight ->
                if (exerciseToEdit == null) {
                    viewModel.addExercise(name, sets, reps, weight)
                } else {
                    viewModel.updateExercise(
                        exerciseToEdit!!.copy(
                            name = name,
                            numberOfSets = sets,
                            repsPerSet = reps,
                            weightAmount = weight
                        )
                    )
                }
                showExerciseDialog = false
            }
        )
    }
}

@Composable
fun WorkoutItem(workout: Workout, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Session on ${df.format(workout.workoutDate)}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${exercise.numberOfSets} sets x ${exercise.repsPerSet} reps @ ${exercise.weightAmount} lbs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDialog(
    initialExercise: Exercise?,
    availableNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Double) -> Unit
) {
    var name by remember { mutableStateOf(initialExercise?.name ?: "") }
    var sets by remember { mutableStateOf(initialExercise?.numberOfSets?.toString() ?: "") }
    var reps by remember { mutableStateOf(initialExercise?.repsPerSet?.toString() ?: "") }
    var weight by remember { mutableStateOf(initialExercise?.weightAmount?.toString() ?: "") }

    var expanded by remember { mutableStateOf(false) }

    var isCreatingNew by remember {
        mutableStateOf(initialExercise != null && !availableNames.contains(initialExercise.name))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialExercise == null) "Add Exercise" else "Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                if (isCreatingNew || availableNames.isEmpty()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Exercise Name") },
                        trailingIcon = {
                            if (availableNames.isNotEmpty()) {
                                IconButton(onClick = {
                                    isCreatingNew = false
                                    name = availableNames.firstOrNull() ?: ""
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Select from list")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = name.ifEmpty { "Select Exercise" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exercise Name") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableNames.forEach { exName ->
                                DropdownMenuItem(
                                    text = { Text(exName) },
                                    onClick = {
                                        name = exName
                                        expanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Add new exercise...", color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    isCreatingNew = true
                                    name = ""
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = sets.toIntOrNull() ?: 0
                    val r = reps.toIntOrNull() ?: 0
                    val w = weight.toDoubleOrNull() ?: 0.0
                    onSave(name, s, r, w)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}