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
import com.smartnas.app.ui.components.*
import com.smartnas.app.ui.screens.home.MainBottomBar
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
    var keyword by remember { mutableStateOf("") }
    var selectedTagId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPhotos()
        viewModel.loadTags()
    }

    Scaffold(
        topBar = {
            SmartTopBar(title = "照片总览", onBack = { navController.popBackStack() })
        },
        bottomBar = { MainBottomBar(navController) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text("搜索照片...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        if (keyword.isNotBlank()) viewModel.searchPhotos(keyword)
                        else viewModel.loadPhotos()
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "搜索")
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tag Chips
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTagId == null,
                        onClick = { selectedTagId = null; viewModel.loadPhotos() },
                        label = { Text("全部") }
                    )
                    tags.take(4).forEach { tag ->
                        FilterChip(
                            selected = selectedTagId == tag.id,
                            onClick = {
                                selectedTagId = if (selectedTagId == tag.id) null else tag.id
                                viewModel.loadPhotos(tagId = selectedTagId)
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            // Photo Grid
            when (val p = photos) {
                Resource.Idle -> {}
                is Resource.Loading -> LoadingScreen()
                is Resource.Error -> ErrorRetry(p.message, onRetry = { viewModel.refresh() })
                is Resource.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(p.data.records) { photo ->
                            val context = LocalContext.current
                            val imageUrl = viewModel.baseUrlHolder.photoThumbUrl(photo.id)
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = photo.name,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { navController.navigate(Routes.photoDetail(photo.id)) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
