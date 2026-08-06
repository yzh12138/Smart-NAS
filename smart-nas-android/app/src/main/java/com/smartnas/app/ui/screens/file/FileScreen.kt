package com.smartnas.app.ui.screens.file

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.FileStorage
import com.smartnas.app.data.model.PageResult
import com.smartnas.app.ui.components.*
import com.smartnas.app.ui.navigation.Routes
import com.smartnas.app.util.Resource
import com.smartnas.app.util.formatFileSize
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _files = MutableStateFlow<Resource<PageResult<FileStorage>>>(Resource.Loading)
    val files: StateFlow<Resource<PageResult<FileStorage>>> = _files

    fun loadFiles() {
        viewModelScope.launch {
            _files.value = Resource.Loading
            try {
                val resp = api.getFileList()
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _files.value = Resource.Success(resp.body()!!.data!!)
                } else {
                    _files.value = Resource.Error(resp.body()?.message ?: "加载失败")
                }
            } catch (e: Exception) {
                _files.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun uploadFile(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val tempFile = File.createTempFile("upload_", ".bin", context.cacheDir)
                tempFile.outputStream().use { inputStream.copyTo(it) }
                val requestBody = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)
                api.uploadFile(part)
                loadFiles()
            } catch (_: Exception) {}
        }
    }

    fun deleteFile(id: Long) {
        viewModelScope.launch {
            try {
                api.deleteFile(id)
                loadFiles()
            } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(
    navController: NavController,
    viewModel: FileViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val files by viewModel.files.collectAsStateWithLifecycle()
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadFile(it, context) } }

    LaunchedEffect(Unit) { viewModel.loadFiles() }

    Scaffold(
        topBar = { SmartTopBar(title = "文件存储", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickerLauncher.launch("*/*") }) {
                Icon(Icons.Default.Upload, contentDescription = "上传文件")
            }
        }
    ) { padding ->
        when (val f = files) {
            Resource.Idle -> {}
            is Resource.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is Resource.Error -> ErrorRetry(f.message, onRetry = { viewModel.loadFiles() }, modifier = Modifier.padding(padding))
            is Resource.Success -> {
                if (f.data.records.isEmpty()) {
                    EmptyState(icon = Icons.Default.Folder, title = "暂无文件", modifier = Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(f.data.records) { file ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.originalName, style = MaterialTheme.typography.bodyLarge)
                                        Text(formatFileSize(file.fileSize), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.deleteFile(file.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
