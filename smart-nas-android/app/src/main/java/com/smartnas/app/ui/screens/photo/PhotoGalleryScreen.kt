package com.smartnas.app.ui.screens.photo

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
import com.smartnas.app.data.model.Photo
import com.smartnas.app.data.model.Tag
import com.smartnas.app.ui.components.*
import com.smartnas.app.ui.navigation.Routes
import com.smartnas.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    navController: NavController,
    viewModel: PhotoViewModel = hiltViewModel()
) {
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadPhotos()
        viewModel.loadTags()
    }

    LaunchedEffect(selectedTag) {
        viewModel.loadPhotos(tag = selectedTag)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("照片总览") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { navController.navigate(Routes.PHOTO_UPLOAD) }) {
                        Icon(Icons.Default.Add, contentDescription = "上传")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search Bar
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isNotEmpty()) viewModel.searchPhotos(it) else viewModel.loadPhotos()
                    },
                    placeholder = { Text("搜索照片...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { searchQuery = ""; showSearch = false; viewModel.loadPhotos() }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Tag Filter Chips
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { selectedTag = null },
                        label = { Text("全部") }
                    )
                    tags.take(4).forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag.name,
                            onClick = { selectedTag = if (selectedTag == tag.name) null else tag.name },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            // Photo Grid
            when {
                isLoading -> LoadingScreen()
                photos.isEmpty() -> EmptyState(Icons.Default.Photo, "暂无照片", "点击右上角 + 上传照片")
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(photos) { photo ->
                            PhotoGridItem(photo, navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGridItem(photo: Photo, navController: NavController) {
    val context = LocalContext.current
    val baseUrl = "http://10.0.2.2:8080"
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
            .clip(Roun