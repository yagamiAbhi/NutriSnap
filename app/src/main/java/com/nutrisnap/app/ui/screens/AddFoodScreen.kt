package com.nutrisnap.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nutrisnap.app.data.gemini.TemporaryMealPhoto

@Composable
fun AddFoodScreen(
    onManual: () -> Unit,
    onSavedFoods: () -> Unit,
    onPhotoSelected: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPhotoSelected)
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = pendingCaptureUri
        if (captured && uri != null) onPhotoSelected(uri)
        else {
            uri?.let { TemporaryMealPhoto.deleteIfTemporary(context, it) }
            cameraError = "Photo capture was cancelled. You can retry or choose a photo."
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = TemporaryMealPhoto.createCaptureUri(context)
            pendingCaptureUri = uri
            cameraCapture.launch(uri)
            cameraError = null
        } else {
            cameraError = "Camera permission was denied. Choose a photo or enter foods manually."
        }
    }
    val openCamera = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = TemporaryMealPhoto.createCaptureUri(context)
            pendingCaptureUri = uri
            cameraCapture.launch(uri)
            cameraError = null
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add to your log", style = MaterialTheme.typography.headlineMedium)
        Text("Choose how you want to add this meal.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = openCamera, modifier = Modifier.weight(1f).height(56.dp)) {
                androidx.compose.material3.Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Text("  Take photo")
            }
            OutlinedButton(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                androidx.compose.material3.Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Text("  Choose")
            }
        }
        cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.EditNote, contentDescription = null)
            Text("  Enter manually")
        }
        OutlinedButton(onClick = onSavedFoods, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.Favorite, contentDescription = null)
            Text("  Saved foods")
        }
    }
}
