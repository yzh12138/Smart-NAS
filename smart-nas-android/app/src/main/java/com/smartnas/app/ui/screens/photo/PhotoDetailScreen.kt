package com.smartnas.app.ui.screens.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val detail by viewModel.photoDetail.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(photoId) {
        viewModel.getPhotoDetail(photoId)
        viewModel.loadComments(photoId)
        viewModel.trackClick(photoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("照片详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showComments = !showComments }) {
                        Icon(Icons.Default.Comment, contentDescription = "评论")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        when (val d = detail) {
            is Resource.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is Resource.Error -> ErrorRetry(d.message, onRetry = { viewModel.getPhotoDetail(photoId) }, modifier = Modifier.padding(padding))
            is Resource.Success -> {
                val photo = d.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Full Image
                    item {
                        val baseUrl = "http://10.0.2.2:8080"
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("$baseUrl/api/photo/${photo.id}/original")
                                .crossfade(true)
                                .build(),
                            contentDescription = photo.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(photo.width.toFloat().coerceAtLeast(1f) / photo.height.toFloat().coerceAtLeast(1f).coerceIn(0.5f, 2f))
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Info Card
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(photo.name, style = MaterialTheme.typography.titleLarge)
                                if (!photo.city.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(photo.city!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (!photo.shootTime.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(photo.shootTime!!, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${photo.clickCount} 次查看", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Tags
                    if (photo.tags.isNotEmpty()) {
                        item {
                            Text("标签", style = MaterialTheme.typography.titleMedium)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                photo.tags.forEach { tag ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(tag.name) }
                                    )
                                }
                            }
                        }
                    }

                    // AI Tags
                    if (!photo.aiTags.isNullOrBlank()) {
                        item {
                            Text("AI 标签", style = MaterialTheme.typography.titleMedium)
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Text(
                                    photo.aiTags!!,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Comments Section
                    if (showComments) {
                        item {
                            Text("留言 (${comments.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        items(comments) { comment ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                comment.nickname.firstOrNull()?.toString() ?: "?",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(comment.nickname, style = MaterialTheme.typography.bodyMedium)
                                            Text(comment.createTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        // Add comment
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = { Text("添加留言...") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            viewModel.addComment(photoId, commentText)
                                            commentText = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "发送", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除照片") },
                text = { Text("确定要删除这张照片吗？照片将移入回收站。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deletePhoto(photoId) { success ->
                            if (success) navController.popBackStack()
                        }
                        showDeleteDialog = false
                    }) { Text("删除", colo