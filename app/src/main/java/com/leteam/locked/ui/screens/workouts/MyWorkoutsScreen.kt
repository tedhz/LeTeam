package com.leteam.locked.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leteam.locked.workout.Exercise
import com.leteam.locked.workout.Workout
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWorkoutsScreen(
    viewModel: MyWorkoutsViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val workouts by viewModel.workouts.collectAsState()
    val selectedWorkout by viewModel.selectedWorkout.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val availableExerciseNames by viewModel.availableExerciseNames.collectAsState()

    var showExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }

    if (selectedWorkout == null) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                TopAppBar(
                    title = { Text("My Workouts", fontWeight = FontWeight.Black, color = Color.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.createWorkout() },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Workout", tint = Color.White) },
                    text = { Text("New Session", color = Color.White, fontWeight = FontWeight.Bold) },
                    containerColor = Color.Black
                )
            }
        ) { padding ->
            if (workouts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Event,
                    message = "No workouts found.\nTap 'New Session' to start.",
                    modifier = Modifier.padding(padding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(workouts) { workout ->
                        WorkoutItem(workout) { viewModel.selectWorkout(workout) }
                    }
                }
            }
        }
    } else {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                TopAppBar(
                    title = { Text("Workout Details", fontWeight = FontWeight.Black, color = Color.Black) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelectedWorkout() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        exerciseToEdit = null
                        showExerciseDialog = true
                    },
                    containerColor = Color.Black,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
            }
        ) { padding ->
            if (exercises.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FitnessCenter,
                    message = "Your session is empty.\nAdd your first exercise!",
                    modifier = Modifier.padding(padding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
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
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutItem(workout: Workout, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("EEEE, MMM dd • h:mm a", Locale.getDefault()) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Event,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = df.format(workout.workoutDate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.DarkGray
            )
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise, onEdit: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.Black), // High contrast border for exercises
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Black)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                ExerciseMetric(label = "Sets", value = exercise.numberOfSets.toString())
                ExerciseMetric(label = "Reps", value = exercise.repsPerSet.toString())
                ExerciseMetric(label = "Weight", value = "${trimWeight(exercise.weightAmount)} lbs")
            }
        }
    }
}

@Composable
fun ExerciseMetric(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun trimWeight(w: Double): String {
    val asInt = w.toInt()
    return if (w == asInt.toDouble()) asInt.toString() else w.toString()
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
        containerColor = Color.White,
        onDismissRequest = onDismiss,
        title = { Text(if (initialExercise == null) "Add Exercise" else "Edit Exercise", fontWeight = FontWeight.Black, color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                if (isCreatingNew || availableNames.isEmpty()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Exercise Name", color = Color.DarkGray) },
                        trailingIcon = {
                            if (availableNames.isNotEmpty()) {
                                IconButton(onClick = {
                                    isCreatingNew = false
                                    name = availableNames.firstOrNull() ?: ""
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Select from list", tint = Color.Black)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
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
                            label = { Text("Exercise Name", color = Color.DarkGray) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            containerColor = Color.White
                        ) {
                            availableNames.forEach { exName ->
                                DropdownMenuItem(
                                    text = { Text(exName, color = Color.Black) },
                                    onClick = {
                                        name = exName
                                        expanded = false
                                    }
                                )
                            }
                            HorizontalDivider(color = Color(0xFFE0E0E0))
                            DropdownMenuItem(
                                text = { Text("+ Add new exercise...", color = Color.Black, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    isCreatingNew = true
                                    name = ""
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text("Sets", color = Color.DarkGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps", color = Color.DarkGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (lbs)", color = Color.DarkGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
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
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}