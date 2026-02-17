package com.docvault.ui.screens

import android.graphics.*
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDocumentScreen(
    imageUri: Uri,
    onSave: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // States for editing tools
    var rotation by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }

    LaunchedEffect(imageUri) {
        context.contentResolver.openInputStream(imageUri)?.use {
            originalBitmap = BitmapFactory.decodeStream(it)
            currentBitmap = originalBitmap
        }
    }

    // Effect to apply filters in real-time
    LaunchedEffect(rotation, contrast, brightness, saturation) {
        originalBitmap?.let { src ->
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
            
            val dest = Bitmap.createBitmap(rotated.width, rotated.height, rotated.config)
            val canvas = Canvas(dest)
            val paint = Paint()
            
            val cm = ColorMatrix()
            cm.setSaturation(saturation)
            
            // Contrast & Brightness matrix
            val c = contrast
            val b = brightness
            val contrastMatrix = floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )
            cm.postConcat(ColorMatrix(contrastMatrix))
            
            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(rotated, 0f, 0f, paint)
            currentBitmap = dest
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        currentBitmap?.let {
                            val file = File(context.cacheDir, "edt_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(file).use { out -> it.compress(Bitmap.CompressFormat.JPEG, 95, out) }
                            onSave(Uri.fromFile(file))
                        }
                    }) {
                        Icon(Icons.Outlined.Check, "Save")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { rotation += 90f }) { Icon(Icons.AutoMirrored.Outlined.RotateRight, "Rotate") }
                    IconButton(onClick = { saturation = if (saturation == 0f) 1f else 0f }) { 
                        Icon(if (saturation == 0f) Icons.Outlined.ColorLens else Icons.Outlined.Contrast, "B&W Toggle") 
                    }
                    IconButton(onClick = { contrast += 0.2f }) { Icon(Icons.Outlined.ExposurePlus1, "Contrast +") }
                    IconButton(onClick = { contrast -= 0.2f }) { Icon(Icons.Outlined.ExposureNeg1, "Contrast -") }
                    IconButton(onClick = { brightness += 10f }) { Icon(Icons.Outlined.LightMode, "Brightness +") }
                    IconButton(onClick = { brightness -= 10f }) { Icon(Icons.Outlined.DarkMode, "Brightness -") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            currentBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } ?: CircularProgressIndicator()
        }
    }
}
