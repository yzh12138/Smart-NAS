package com.smartnas.app.ui.screens.book

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
import com.smartnas.app.data.model.Book
import com.smartnas.app.data.model.PageResult
import com.smartnas.app.ui.components.*
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
class BookViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _books = MutableStateFlow<Resource<PageResult<Book>>>(Resource.Loading)
    val books: StateFlow<Resource<PageResult<Book>>> = _books

    fun loadBooks() {
        viewModelScope.launch {
            _books.value = Resource.Loading
            try {
                val resp = api.getBookList()
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _books.value = Resource.Success(resp.body()!!.data!!)
                } else {
                    _books.value = Resource.Error(resp.body()?.message ?: "加载失败")
                }
            } catch (e: Exception) {
                _books.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun uploadBook(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val tempFile = File.createTempFile("book_", ".epub", context.cacheDir)
                tempFile.outputStream().use { inputStream.copyTo(it) }
                val requestBody = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)
                api.uploadBook(part)
                loadBooks()
            } catch (_: Exception) {}
        }
    }

    fun deleteBook(id: Long) {
        viewModelScope.launch {
            try {
                api.deleteBook(id)
                loadBooks()
            } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    navController: NavController,
    viewModel: BookViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val books by viewModel.books.collectAsStateWithLifecycle()
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadBook(it, context) } }

    LaunchedEffect(Unit) { viewModel.loadBooks() }

    Scaffold(
        topBar = { SmartTopBar(title = "图书管理", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickerLauncher.launch("*/*") }) {
                Icon(Icons.Default.Upload, contentDescription = "上传图书")
            }
        }
    ) { padding ->
        when (val b = books) {
            Resource.Idle -> {}
            is Resource.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is Resource.Error -> ErrorRetry(b.message, onRetry = { viewModel.loadBooks() }, modifier = Modifier.padding(padding))
            is Resource.Success -> {
                if (b.data.records.isEmpty()) {
                    EmptyState(icon = Icons.Default.MenuBook, title = "暂无图书", modifier = Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(b.data.records) { book ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(book.title, style = MaterialTheme.typography.titleMedium)
                                        if (book.author.isNotBlank()) {
                                            Text(book.author, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(formatFileSize(book.fileSize), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.deleteBook(book.id) }) {
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
