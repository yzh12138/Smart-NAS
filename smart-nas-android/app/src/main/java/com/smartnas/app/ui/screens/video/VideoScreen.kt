package com.smartnas.app.ui.screens.video

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
class VideoViewModel @Inject constructor(private val api: SmartNASApi, val baseUrlHolder: BaseUrlHolder) : androidx.lifecycle.ViewModel() {
    private val _videos = MutableStateFlow<Resource<PageResult<Photo>>>(Resource.Loading)
    val videos: StateFlow<Resource<PageResult<Photo>>> = _videos

    fun loadVideos() {
        viewModelScope.launch {
            _videos.value = Resource.Loading
            try {
                val resp = api.getPhotoList(page = 1, size = 50, mediaType = "1")
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _videos.value = Resource.Success(resp.body()!!.data!!)
                } else {
                    _videos.value = Resource.Error(resp.body()?.message ?: "加载失败")
                }
            } catch (e: Exception) {
                _videos.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    navController: NavController,
    viewModel: VideoViewModel = hiltViewModel()
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadVideos() }

    Scaffold(
        topBar = { SmartTopBar(title = "视频管理", onBack = { navController.popBackStack() }) },
        bottomBar = { MainBottomBar(navController) }
    ) { padding ->
        when (val v = videos) {
            Resource.Idle -> {}
            is Resource.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is Resource.Error -> ErrorRetry(v.message, onRetry = { viewModel.loadVideos() }, modifier = Modifier.padding(padding))
            is Resource.Success -> {
                if (v.data.records.isEmpty()) {
                    EmptyState(icon = Icons.Default.Videocam, title = "暂无视频", modifier = Modifier.padding(padding))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(padding).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(v.data.records) { video ->
                            val context = LocalContext.current
                            Card(
                                modifier = Modifier.clickable { navController.navigate(Routes.photoDetail(video.id)) }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(viewModel.baseUrlHolder.photoThumbUrl(video.id))
                                        .crossfade(true).build(),
                                    contentDescription = video.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(video.name, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainBottomBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("首页") }, selected = false,
            onClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } })
        NavigationBarItem(icon = { Icon(Icons.Default.PhotoLibrary, null) }, label = { Text("照片") }, selected = false,
            onClick = { navController.navigate(Routes.PHOTO_GALLERY) })
        NavigationBarItem(icon = { Icon(Icons.Default.SmartToy, null) }, label = { Text("AI") }, selected = false,
            onClick = { navController.navigate(Routes.AI_CHAT) })
        NavigationBarItem(icon = { Icon(Icons.Default.Folder, null) }, label = { Text("文件") }, selected = false,
            onClick = { navController.navigate(Routes.FILE) })
        NavigationBarItem(icon = { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("更多") }, selected = false,
            onClick = { navController.navigate(Routes.SETTINGS) })
    }
}
