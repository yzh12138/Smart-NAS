package com.smartnas.app.ui.screens.face

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
import com.smartnas.app.data.model.FaceCluster
import com.smartnas.app.data.model.Photo
import com.smartnas.app.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FaceViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _clusters = MutableStateFlow<List<FaceCluster>>(emptyList())
    val clusters: StateFlow<List<FaceCluster>> = _clusters
    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos
    private val _selectedCluster = MutableStateFlow<FaceCluster?>(null)
    val selectedCluster: StateFlow<FaceCluster?> = _selectedCluster

    fun loadClusters() {
        viewModelScope.launch {
            try {
                val resp = api.getFaceClusters()
                if (resp.isSuccessful && resp.body()?.code == 0) _clusters.value = resp.body()?.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun selectCluster(cluster: FaceCluster) {
        _selectedCluster.value = cluster
        viewModelScope.launch {
            try {
                val resp = api.getFaceClusterPhotos(cluster.id)
                if (resp.isSuccessful && resp.body()?.code == 0) _photos.value = resp.body()?.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun renameCluster(id: Long, name: String) {
        viewModelScope.launch {
            try { api.renameFaceCluster(id, mapOf("name" to name)); loadClusters() } catch (_: Exception) {}
        }
    }

    fun deleteCluster(id: Long) {
        viewModelScope.launch {
            try { api.deleteFaceCluster(id); loadClusters(); _selectedCluster.value = null } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceScreen(navController: NavController, viewModel: FaceViewModel = hiltViewModel()) {
    val clusters by viewModel.clusters.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val selected by viewModel.selectedCluster.collectAsStateWithLifecycle()
    var renameDialogId by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadClusters() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected?.name ?: "人脸识别") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selected != null) viewModel.selectCluster(null as FaceCluster) else navController.popBackStack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (selected == null) {
            if (clusters.isEmpty()) {
                EmptyState(Icons.Default.Face, "暂无人脸聚类", "上传照片后系统会自动识别", modifier = Modifier.padding(padding))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clusters) { cluster ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectCluster(cluster) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Face, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(cluster.name.ifEmpty { "未命名" }, style = MaterialTheme.typography.titleSmall)
                                Text("${cluster.photoCount} 张照片", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row {
                                    IconButton(onClick = { renameDialogId = cluster.id; renameText = cluster.name }) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.deleteCluster(cluster.id) }) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (photos.isEmpty()) {
                EmptyState(Icons.Default.Photo, "暂无照片")
            } else {
                val baseUrl = "http://10.0.2.2:8080"
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(photos) { photo ->
                        AsyncImage(
                            model = "$baseUrl/api/photo/${photo.id}/thumb",
                            contentDescription = null,
                            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).clickable {
                                navController.navigate(com.smartnas.app.ui.navigation.Routes.photoDetail(photo.id))
                            },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        renameDialogId?.let { id ->
            AlertDialog(
                onDismissRequest = { renameDialogId = null },
                title = { Text("重命名") },
                text = { OutlinedTextField(renameText, { renameText = it }, label = { Text("名称") }, singleLine = true) },
                confirmButton = { TextButton(onClick = { viewModel.renameCluster(id, renameText); renameDialogId =