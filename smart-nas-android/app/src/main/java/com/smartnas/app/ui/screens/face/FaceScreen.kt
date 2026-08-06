package com.smartnas.app.ui.screens.face

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
import com.smartnas.app.data.model.FaceCluster
import com.smartnas.app.data.model.Photo
import com.smartnas.app.ui.components.*
import com.smartnas.app.util.Resource
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FaceViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _clusters = MutableStateFlow<List<FaceCluster>>(emptyList())
    val clusters: StateFlow<List<FaceCluster>> = _clusters
    private val _clusterPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val clusterPhotos: StateFlow<List<Photo>> = _clusterPhotos

    fun loadClusters() {
        viewModelScope.launch {
            try {
                val resp = api.getFaceClusters()
                if (resp.isSuccessful && resp.body()?.code == 200) _clusters.value = resp.body()!!.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun loadClusterPhotos(id: Long) {
        viewModelScope.launch {
            try {
                val resp = api.getFaceClusterPhotos(id)
                if (resp.isSuccessful && resp.body()?.code == 200) _clusterPhotos.value = resp.body()!!.data ?: emptyList()
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
            try { api.deleteFaceCluster(id); loadClusters() } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceScreen(
    navController: NavController,
    viewModel: FaceViewModel = hiltViewModel()
) {
    val clusters by viewModel.clusters.collectAsStateWithLifecycle()
    var selectedCluster by remember { mutableStateOf<FaceCluster?>(null) }
    var renameDialogId by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadClusters() }

    Scaffold(
        topBar = { SmartTopBar(title = "人脸识别", onBack = { navController.popBackStack() }) }
    ) { padding ->
        if (clusters.isEmpty()) {
            EmptyState(icon = Icons.Default.Face, title = "暂无人脸聚类", modifier = Modifier.padding(padding))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clusters) { cluster ->
                    Card(
                        modifier = Modifier.clickable { selectedCluster = cluster; viewModel.loadClusterPhotos(cluster.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(cluster.name.ifBlank { "未命名" }, style = MaterialTheme.typography.titleSmall)
                            Text("${cluster.photoCount} 张照片", style = MaterialTheme.typography.bodySmall)
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
    }

    if (renameDialogId != null) {
        AlertDialog(
            onDismissRequest = { renameDialogId = null },
            title = { Text("重命名") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("名称") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.renameCluster(renameDialogId!!, renameText); renameDialogId = null }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { renameDialogId = null }) { Text("取消") } }
        )
    }
}
