package com.smartnas.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.smartnas.app.data.model.Photo
import com.smartnas.app.ui.components.*
import com.smartnas.app.ui.navigation.Routes
import com.smartnas.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart NAS") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Icon(Icons.Default.Person, contentDescription = "个人")
                    }
                }
            )
        },
        bottomBar = { MainBottomBar(navController) }
    ) { padding ->
        when (val s = stats) {
            Resource.Idle -> {}
            is Resource.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is Resource.Error -> ErrorRetry(s.message, onRetry = { viewModel.loadDashboard() }, modifier = Modifier.padding(padding))
            is Resource.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "照片",
                            value = "${s.data.photoCount}",
                            icon = Icons.Default.Photo,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Routes.PHOTO_GALLERY) }
                        )
                        StatCard(
                            title = "视频",
                            value = "${s.data.videoCount}",
                            icon = Icons.Default.Videocam,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Routes.VIDEO) }
                        )
                    }

                    // Quick Actions
                    Text("快捷操作", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { navController.navigate(Routes.PHOTO_UPLOAD) },
                            label = { Text("上传照片") },
                            leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null, Modifier.size(18.dp)) }
                        )
                        AssistChip(
                            onClick = { navController.navigate(Routes.AI_CHAT) },
                            label = { Text("AI 对话") },
                            leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, Modifier.size(18.dp)) }
                        )
                        AssistChip(
                            onClick = { navController.navigate(Routes.FILE) },
                            label = { Text("文件") },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, Modifier.size(18.dp)) }
                        )
                    }

                    // City Stats
                    if (s.data.cityStats.isNotEmpty()) {
                        Text("城市分布", style = MaterialTheme.typography.titleMedium)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                s.data.cityStats.take(5).forEach { city ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navController.navigate(Routes.PHOTO_GALLERY) }
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(city.city, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${city.count} 张",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recent Photos
                    if (recentPhotos.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("最近照片", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { navController.navigate(Routes.PHOTO_GALLERY) }) {
                                Text("查看全部")
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.height(300.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(recentPhotos) { photo ->
                                PhotoThumbnail(photo, navController, viewModel.baseUrlHolder.baseUrl)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoThumbnail(photo: Photo, navController: NavController, baseUrl: String = "http://10.0.2.2:8080") {
    val context = LocalContext.current
    val imageUrl = if (!photo.thumbnailPath.isNullOrBlank()) {
        "$baseUrl/api/photo/${photo.id}/thumb"
    } else {
        "$baseUrl/api/photo/${photo.id}/original"
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = photo.name,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { navController.navigate(Routes.photoDetail(photo.id)) },
        contentScale = ContentScale.Crop
    )
}

@Composable
fun MainBottomBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("首页") },
            selected = true,
            onClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
            label = { Text("照片") },
            selected = false,
            onClick = { navController.navigate(Routes.PHOTO_GALLERY) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
            label = { Text("AI") },
            selected = false,
            onClick = { navController.navigate(Routes.AI_CHAT) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
            label = { Text("文件") },
            selected = false,
            onClick = { navController.navigate(Routes.FILE) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
            label = { Text("更多") },
            selected = false,
            onClick = { navController.navigate(Routes.SETTINGS) }
        )
    }
}
