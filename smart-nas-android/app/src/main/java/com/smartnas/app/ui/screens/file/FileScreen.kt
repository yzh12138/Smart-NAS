package com.smartnas.app.ui.screens.file

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.FileStorage
import com.smartnas.app.ui.components.*
import com.smartnas.app.util.formatFileSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _files = MutableStateFlow<List<FileStorage>>(emptyList())
    val files: StateFlow<List<FileStorage>> = _files
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadFiles() {
        androidx.lifecycle.viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = api.getFileList()
                if (resp.isSuccessful && resp.body()?.code == 0) _files.value = resp.body()?.data?.records ?: emptyList()
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun uploadFile(file: File, onDone: () -> Unit) {
        androidx.lifecycle.viewModelScope.launch {
            try {
                val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("*/*".toMediaTypeOrNull()))
                api.uploadFile(part)
                loadFiles()
                onDone()
            } catch (_: Exception) {}
        }
    }

    fun deleteFile(id: Long) {
        androidx.lifecycle.viewModelScope.launch {
            try { api.deleteFile(id); loadFiles() } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(navController: NavController, viewModel: FileViewModel = hiltViewModel()) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var uploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploading = true
            val file = File.createTempFile("file_", ".tmp", context.cacheDir)
            context.contentResolver.openInputStream(it)!!.use { input -> file.outputStream().use { out -> input.copyTo(out) } }
            viewModel.uploadFile(file) { uploading = false }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadFiles() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件存储") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = { launcher.launch("*/*") }) { Icon(Icons.Default.Upload, contentDescription = "上传") } }
            )
        }
    ) { padding ->
        when {
            isLoading || uploading -> LoadingScreen(modifier = Modifier.padding(padding))
            files.isEmpty() -> EmptyState(Icons.Default.Folder, "暂无文件", "点击右上角上传", modifier = Modifier.padding(padding))
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files) { file ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.originalName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text("${formatFileSize(file.fileSize)} · ${file.uploadTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteFile(file.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                }
           