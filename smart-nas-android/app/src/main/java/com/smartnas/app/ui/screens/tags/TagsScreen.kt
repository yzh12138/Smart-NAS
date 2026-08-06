package com.smartnas.app.ui.screens.tags

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.Tag
import com.smartnas.app.ui.components.*
import com.smartnas.app.util.Resource
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags

    fun loadTags() {
        viewModelScope.launch {
            try {
                val resp = api.getTagList()
                if (resp.isSuccessful && resp.body()?.code == 200) _tags.value = resp.body()!!.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun createTag(name: String, color: String) {
        viewModelScope.launch {
            try { api.createTag(mapOf("name" to name, "color" to color)); loadTags() } catch (_: Exception) {}
        }
    }

    fun updateTag(id: Long, name: String, color: String) {
        viewModelScope.launch {
            try { api.updateTag(id, mapOf("name" to name, "color" to color)); loadTags() } catch (_: Exception) {}
        }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch {
            try { api.deleteTag(id); loadTags() } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    navController: NavController,
    viewModel: TagsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadTags() }

    Scaffold(
        topBar = { SmartTopBar(title = "标签管理", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "创建标签")
            }
        }
    ) { padding ->
        if (tags.isEmpty()) {
            EmptyState(icon = Icons.Default.Label, title = "暂无标签", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    modifier = Modifier.size(16.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = try { Color(android.graphics.Color.parseColor(tag.color)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                                ) {}
                                Text(tag.name, style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(onClick = { viewModel.deleteTag(tag.id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("创建标签") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("标签名称") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.createTag(newName, "#409EFF"); newName = ""; showCreateDialog = false }) { Text("创建") } },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("取消") } }
        )
    }
}
