package com.sudhanshu.tva.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.network.VisionRequest
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch

@Composable
fun CameraVisionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var prompt by remember { mutableStateOf("Describe what you see and point out anything important.") }
    var answer by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { captured ->
        bitmap = captured
        answer = ""
        status = if (captured != null) "Image captured. Tap Analyze." else "Camera cancelled."
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
        else status = "Camera permission denied."
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Camera Vision Mode", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Camera is user-triggered only. The captured image is sent to the relay for AI analysis and is not stored by TVA.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Button(
            onClick = {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) cameraLauncher.launch(null)
                else permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open camera") }

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Captured image",
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 12.dp)
            )
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("What should TVA analyze?") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            enabled = bitmap != null,
            onClick = {
                val image = bitmap ?: return@Button
                scope.launch {
                    status = "Analyzing…"
                    try {
                        val out = ByteArrayOutputStream()
                        image.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        val res = RelayClient.api.analyzeImage(
                            VisionRequest(prompt, encoded, "image/jpeg")
                        )
                        answer = if (res.isSuccessful) res.body()?.answer ?: "No answer"
                            else "Relay error ${res.code()}"
                        status = "Done"
                    } catch (e: Exception) {
                        status = e.message ?: "Vision failed"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Analyze image") }

        Text(status, modifier = Modifier.padding(top = 8.dp))
        if (answer.isNotBlank()) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(answer, Modifier.padding(16.dp))
            }
        }
    }
}
