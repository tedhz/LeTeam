package com.leteam.locked.ui.screens.camera

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import androidx.compose.runtime.saveable.rememberSaveable



private fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "photo_${System.currentTimeMillis()}.jpg")

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Composable
fun CameraScreen(
    onBackClick: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val photoUri by viewModel.photoUri.collectAsState()

    var launchedOnce by rememberSaveable { mutableStateOf(false) }

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
            val uri = createImageUri(context)
            viewModel.setPhotoUri(uri)
            takePictureLauncher.launch(uri)
        } else {
            onBackClick()
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = if (photoUri == null) "Opening camera..." else "Photo captured!",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onBackClick) {
                Text("Go Back")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { launchCameraFlow() }) {
                Text("Retake Photo")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {

                },
                enabled = photoUri != null
            ) {
                Text("Post Screen for Ted's Part")
            }
        }
    }
}
