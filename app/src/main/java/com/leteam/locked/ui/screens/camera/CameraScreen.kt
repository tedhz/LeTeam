package com.leteam.locked.ui.screens.camera

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

private fun createImageUri(context: Context): Uri {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LeTeamLocked")
        }
    }
    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: throw IllegalStateException("Failed to create MediaStore entry")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBackClick: () -> Unit,
    onPostClick: (Uri) -> Unit = {},
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val photoUri by viewModel.photoUri.collectAsState()
    var launchedOnce by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var captureError by rememberSaveable { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = pendingUri
            if (uri != null) {
                viewModel.setPhotoUri(uri)
                captureError = null
            } else {
                viewModel.setPhotoUri(null)
                captureError = "Capture failed."
            }
        } else {
            pendingUri?.let { context.contentResolver.delete(it, null, null) }
            viewModel.setPhotoUri(null)
        }
        pendingUri = null
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) {
            captureError = null
            launchCameraFlow(context) { uri ->
                pendingUri = uri
                takePictureLauncher.launch(uri)
            }
        }
    }

    fun retake() {
        captureError = null
        launchCameraFlow(
            context = context,
            onRequestPermission = { requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
            onPermissionDenied = { permissionDenied = true },
            onUriReady = { uri ->
                pendingUri = uri
                takePictureLauncher.launch(uri)
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!launchedOnce) {
            launchedOnce = true
            retake()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Capture", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when {
                permissionDenied -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text("Camera access required", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Enable Camera")
                        }
                    }
                }
                photoUri == null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (captureError != null) {
                            Text(text = captureError!!, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { retake() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Try Again")
                            }
                        } else {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                else -> {
                    val uri = photoUri!!
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .aspectRatio(4f / 5f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Captured photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { retake() },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retake", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            }
                            IconButton(
                                onClick = { onPostClick(uri) },
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(MaterialTheme.colorScheme.onBackground, CircleShape)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Next", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun launchCameraFlow(
    context: Context,
    onRequestPermission: (() -> Unit)? = null,
    onPermissionDenied: (() -> Unit)? = null,
    onUriReady: (Uri) -> Unit
) {
    val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    if (hasPermission) {
        onUriReady(createImageUri(context))
    } else {
        onRequestPermission?.invoke() ?: onPermissionDenied?.invoke()
    }
}