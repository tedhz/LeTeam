package com.leteam.locked.ui.screens.camera

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                captureError = "Capture succeeded but no URI was available."
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
            launchCameraFlow(
                context = context,
                onUriReady = { uri ->
                    pendingUri = uri
                    takePictureLauncher.launch(uri)
                }
            )
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
        topBar = {
            TopAppBar(
                title = { Text("New Post") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { retake() },
                    enabled = !permissionDenied,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retake")
                }

                Button(
                    onClick = { photoUri?.let(onPostClick) },
                    enabled = photoUri != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                permissionDenied -> {
                    Text(
                        "Camera permission denied. Enable it in Settings to take a photo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }) {
                        Text("Try Again")
                    }
                }

                photoUri == null -> {
                    if (captureError != null) {
                        Text(
                            text = captureError!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Text("Opening camera...", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    val uri = photoUri!!


                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(uri)
                            .crossfade(true)
                            .memoryCacheKey(uri.toString())
                            .diskCacheKey(uri.toString())
                            .build(),
                        contentDescription = "Captured photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        onError = {
                            android.util.Log.e("CameraScreen", "Coil failed to load: $uri", it.result.throwable)
                            captureError = it.result.throwable?.toString() ?: "Failed to load image."
                        }
                    )
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
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
        onUriReady(createImageUri(context))
    } else {
        if (onRequestPermission != null) {
            onRequestPermission()
        } else {
            onPermissionDenied?.invoke()
        }
    }
}
