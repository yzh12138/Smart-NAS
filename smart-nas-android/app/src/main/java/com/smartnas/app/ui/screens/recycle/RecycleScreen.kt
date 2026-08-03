package com.smartnas.app.ui.screens.recycle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.Photo
import com.smartnas.app.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecycleViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadRecycle() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = api.getRecycleList()
                if (resp.isSuccessful && resp.body()?.code == 0) _photos.value = resp.body()?.data?.records ?: emptyList()
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            try { api.restorePhoto(id); loadRecycle() } catch (_: Exception) {}
        }
    }

    fun permanentDelete(id: Long) {
        viewModelScope.launch {
            try { api.permanentDelete(id); loadRecycle() } catch (_: Exception) {}
        }
    }

    fun emptyAll() {
        viewModelScope.launch {
            try { api.emptyRecycle(); loadRecycle() } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleScreen(navController: NavController, viewModel: RecycleViewModel = hiltViewModel()) {
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showEmptyDialog by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }

    LaunchedEffect(Unit) { viewModel.loadRecycle() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (photos.isNotEmpty()) {
                        IconButton(onClick = { showEmptyDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> LoadingScreen(modifier = Modifier.padding(padding))
            photos.isEmpty() -> EmptyState(Icons.Default.Recycling, "回收站为空", modifier = Modifier.padding(padding))
            else -> {
                val baseUrl = "http://10.0.2.2:8080"
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(photos) { photo ->
                        Box {
                            AsyncImage(
                                model = "$baseUrl/api/photo/${photo.id}/thumb",
                                contentDescription = null,
                                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).clickable { selectedPhoto = photo },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        selectedPhoto?.let { photo ->
            AlertDialog(
                onDismissRequest = { selectedPhoto = null },
                title = { Text("照片操作") },
                text = { Text("「${photo.name}」") },
                confirmButton = {
                    TextButton(onClick = { viewModel.restore(photo.id); selectedPhoto = null }) { Text("恢复") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.permanentDelete(photo.id); selectedPhoto = null }) {
                        Text("永久删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        if (showEmptyDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyDialog = false },
                title = { Text("清空回收站") },
                text = { Text("确定永久删除所有照片？此操作不可撤销。") },
                confirmButton = { TextButton(onClick = { viewModel.emptyAll(); showEmptyDialog = false }) { Text("清空", color = MaterialTheme.colorScheme.error) 