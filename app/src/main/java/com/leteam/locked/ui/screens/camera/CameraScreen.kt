package com.leteam.locked.ui.screens.camera

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File

private fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "photo_${System.currentTimeMillis()}.jpg")
    imageFile.createNewFile()
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
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

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            viewModel.setPhotoUri(null)
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            val uri = createImageUri(context)
            viewModel.setPhotoUri(uri)
            takePictureLauncher.launch(uri)
        } else {
            permissionDenied = true
        }
    }

    fun launchCameraFlow() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val uri = createImageUri(context)
            viewModel.setPhotoUri(uri)
            takePictureLauncher.launch(uri)
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        if (!launchedOnce) {
            launchedOnce = true
            launchCameraFlow()
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
                    onClick = { launchCameraFlow() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retake")
                }

                Button(
                    onClick = { photoUri?.let(onPostClick) },
                    enabled = photoUri != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Post")
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
                    Button(onClick = { launchCameraFlow() }) {
                        Text("Try Again")
                    }
                }

                photoUri == null -> {
                    Text("Opening camera...", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }

                else -> {
                    val uri = photoUri!!

                    Text(
                        text = "Saved: $uri",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))

                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
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
                        }
                    )
                }
            }
        }
    }
}
