package com.smartnas.app.ui.screens.recycle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import coil.request.ImageRequest
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.PageResult
import com.smartnas.app.data.model.Photo
import com.smartnas.app.ui.components.*
import com.smartnas.app.ui.navigation.Routes
import com.smartnas.app.util.BaseUrlHolder
import com.smartnas.app.util.Resource
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecycleViewModel @Inject constructor(private val api: SmartNASApi, val baseUrlHolder: BaseUrlHolder) : androidx.lifecycle.ViewModel() {
    private val _photos = MutableStateFlow<Resource<PageResult<Photo>>>(Resource.Loading)
    val photos: StateFlow<Resource<PageResult<Photo>>> = _photos

    fun loadRecycle() {
        viewModelScope.launch {
            _photos.value = Resource.Loading
            try {
                val resp = api.getRecycleList()
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _photos.value = Resource.Success(resp.body()!!.data!!)
                } else {
                    _photos.value = Resource.Error(resp.body()?.message ?: "加载失败")
                }
            } catch (e: Exception) {
                _photos.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun restorePhoto(id: Long) {
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
fun RecycleScreen(
    navController: NavController,
    viewModel: RecycleViewModel = hiltViewModel()
) {
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    var showEmptyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadRecycle() }

    Scaffold(
        topBar = {
            SmartTopBar(title = "回收站", onBack = { navController.popBackStack() }, actions = {
                IconButton(onClick = { showEmptyDialog = true }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "清空")
                }
            })
        }
    ) { padding ->
        when (val p = photos) {
            Resource.Idle -> {}
            is Resource.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is Resource.Error -> ErrorRetry(p.message, onRetry = { viewModel.loadRecycle() }, modifier = Modifier.padding(padding))
            is Resource.Success -> {
                if (p.data.records.isEmpty()) {
                    EmptyState(icon = Icons.Default.Delete, title = "回收站为空", modifier = Modifier.padding(padding))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.padding(padding).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(p.data.records) { photo ->
                            Card {
                                Column {
                                    val context = LocalContext.current
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(viewModel.baseUrlHolder.photoThumbUrl(photo.id)).crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { viewModel.restorePhoto(photo.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.permanentDelete(photo.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
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

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("清空回收站") },
            text = { Text("确定永久删除所有照片？此操作不可撤销。") },
            confirmButton = { TextButton(onClick = { viewModel.emptyAll(); showEmptyDialog = false }) { Text("清空", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showEmptyDialog = false }) { Text("取消") } }
        )
    }
}
