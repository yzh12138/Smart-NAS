package com.smartnas.app.ui.screens.photo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.smartnas.app.ui.components.SmartTopBar
import com.smartnas.app.util.Resource
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoUploadScreen(
    navController: NavController,
    viewModel: PhotoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var tags by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedUris = uris }

    LaunchedEffect(uploadState) {
        if (uploadState is Resource.Success) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            SmartTopBar(title = "上传照片", onBack = { navController.popBackStack() })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Select Photos Button
            Button(
                onClick = { pickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择照片 (${selectedUris.size})")
            }

            // Preview Selected Photos
            if (selectedUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedUris) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Tags Input
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("标签（逗号分隔）") },
                placeholder = { Text("风景, 旅行, 美食") },
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // City Input
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("城市") },
                placeholder = { Text("北京") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Upload Button
            Button(
                onClick = {
                    val parts = selectedUris.mapNotNull { uri ->
                        val inputStream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null
                        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
                        tempFile.outputStream().use { inputStream.copyTo(it) }
                        val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("files", tempFile.name, requestBody)
                    }
                    viewModel.uploadPhotos(parts, tags.ifBlank { null }, city.ifBlank { null })
                },
                enabled = selectedUris.isNotEmpty() && uploadState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uploadState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上传")
                }
            }

            if (uploadState is Resource.Error) {
                Text(
                    (uploadState as Resource.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
