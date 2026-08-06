package com.smartnas.app.ui.screens.photo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smartnas.app.ui.components.*
import com.smartnas.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    navController: NavController,
    photoId: Long,
    viewModel: PhotoViewModel = hiltViewModel()
) {
    val photoDetail by viewModel.photoDetail.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    var commentText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(photoId) {
        viewModel.loadPhotoDetail(photoId)
        viewModel.loadComments(photoId)
    }

    Scaffold(
        topBar = {
            SmartTopBar(title = "照片详情", onBack = { navController.popBackStack() }, actions = {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            })
        }
    ) { padding ->
        when (val p = photoDetail) {
            Resource.Idle -> {}
            is Resource.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is Resource.Error -> ErrorRetry(p.message, onRetry = { viewModel.loadPhotoDetail(photoId) }, modifier = Modifier.padding(padding))
            is Resource.Success -> {
                val photo = p.data
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Photo Image
                    item {
                        val context = LocalContext.current
                        val imageUrl = viewModel.baseUrlHolder.photoOriginalUrl(photo.id)
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                            contentDescription = photo.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    // Photo Info
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(photo.name, style = MaterialTheme.typography.titleLarge)
                                if (!photo.city.isNullOrBlank()) {
                                    Text("📍 ${photo.city}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                if (!photo.shootTime.isNullOrBlank()) {
                                    Text("📷 ${photo.shootTime}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (photo.tags.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        photo.tags.forEach { tag ->
                                            AssistChip(onClick = {}, label = { Text(tag.name) })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Comments Section
                    item {
                        Text("留言", style = MaterialTheme.typography.titleMedium)
                    }
                    items(comments) { comment ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(comment.nickname.ifBlank { comment.username }, style = MaterialTheme.typography.labelMedium)
                                Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                                Text(comment.createTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Add Comment
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("写留言...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = {
                                if (commentText.isNotBlank()) {
                                    viewModel.addComment(photoId, commentText)
                                    commentText = ""
                                }
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "发送")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除照片") },
            text = { Text("确定要删除这张照片吗？将移入回收站。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePhoto(photoId)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}
