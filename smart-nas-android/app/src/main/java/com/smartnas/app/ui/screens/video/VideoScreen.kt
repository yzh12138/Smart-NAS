package com.smartnas.app.ui.screens.video

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.smartnas.app.data.model.Photo
import com.smartnas.app.ui.components.*
import com.smartnas.app.ui.screens.photo.PhotoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    navController: NavController,
    viewModel: PhotoViewModel = hiltViewModel()
) {
    val videos by viewModel.photos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showUpload by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadPhotos(mediaType = 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视频管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showUpload = true }) {
                        Icon(Icons.Default.Add, contentDescription = "上传")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> LoadingScreen(modifier = Modifier.padding(padding))
            videos.isEmpty() -> EmptyState(
                Icons.Default.Videocam,
                "暂无视频",
                "点击右上角 + 上传视频",
                modifier = Modifier.padding(padding)
            )
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(videos) { video ->
                        VideoCard(video, navController)
                    }
                }
            }
        }

        if (showUpload) {
            val context = LocalContext.current
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    val file = java.io.File.createTempFile("video_", ".mp4", context.cacheDir)
                    context.contentResolver.openInputStream(it)!!.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    viewModel.uploadPhotos(listOf(file))
                    showUpload = false
                }
            }
            LaunchedEffect(Unit) { launcher.launch("video/*") }
        }
    }
}

@Composable
fun VideoCard(video: Photo, navController: NavController) {
    val baseUrl = "http://10.0.2.2:8080"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(com.smartnas.app.ui.navigation.Routes.photoDetail(video.id)) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = "$baseUrl/api/photo/${video.id}/thumb",
                    contentDescription = video.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(video.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                if (!video.city.isNullOrBlank()) {
                    Text(video.city!!, style = Materi