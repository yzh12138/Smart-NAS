package com.smartnas.app.ui.screens.book

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController

import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.Book
import com.smartnas.app.ui.components.*
import com.smartnas.app.util.formatFileSize
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
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = api.getBookList()
                if (resp.isSuccessful && resp.body()?.code == 0) _books.value = resp.body()?.data?.records ?: emptyList()
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun uploadBook(file: File, title: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("*/*".toMediaTypeOrNull()))
                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                api.uploadBook(part, title = titleBody)
                loadBooks(); onDone()
            } catch (_: Exception) {}
        }
    }

    fun deleteBook(id: Long) {
        viewModelScope.launch {
            try { api.deleteBook(id); loadBooks() } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(navController: NavController, viewModel: BookViewModel = hiltViewModel()) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showUpload by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploading = true
            val file = File.createTempFile("book_", ".epub", context.cacheDir)
            context.contentResolver.openInputStream(it)!!.use { input -> file.outputStream().use { out -> input.copyTo(out) } }
            val name = file.nameWithoutExtension
            viewModel.uploadBook(file, name) { uploading = false; showUpload = false }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadBooks() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图书管理") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { launcher.launch("*/*") }) { Icon(Icons.Default.Add, null) } }
            )
        }
    ) { padding ->
        when {
            isLoading || uploading -> LoadingScreen(modifier = Modifier.padding(padding))
            books.isEmpty() -> EmptyState(Icons.Default.MenuBook, "暂无图书", "上传 EPUB 或 PDF 文件", modifier = Modifier.padding(padding))
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(books) { book ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(50.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${book.fileType.uppercase()} · ${formatFileSize(book.fileSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteBook(book.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                             