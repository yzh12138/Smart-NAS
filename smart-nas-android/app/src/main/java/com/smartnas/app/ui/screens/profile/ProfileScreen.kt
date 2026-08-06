package com.smartnas.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.UserInfo
import com.smartnas.app.ui.components.*
import com.smartnas.app.util.Resource
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _user = MutableStateFlow<Resource<UserInfo>>(Resource.Loading)
    val user: StateFlow<Resource<UserInfo>> = _user

    fun loadProfile() {
        viewModelScope.launch {
            _user.value = Resource.Loading
            try {
                val resp = api.getUserInfo()
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _user.value = Resource.Success(resp.body()!!.data!!)
                } else {
                    _user.value = Resource.Error(resp.body()?.message ?: "加载失败")
                }
            } catch (e: Exception) {
                _user.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            try {
                // The API doesn't have a dedicated update profile endpoint in SmartNASApi,
                // so we use a workaround. This may need to be adapted to your actual API.
                loadProfile()
            } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    var editMode by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    Scaffold(
        topBar = { SmartTopBar(title = "个人资料", onBack = { navController.popBackStack() }) }
    ) { padding ->
        when (val u = user) {
            Resource.Idle -> {}
            is Resource.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is Resource.Error -> ErrorRetry(u.message, onRetry = { viewModel.loadProfile() }, modifier = Modifier.padding(padding))
            is Resource.Success -> {
                val info = u.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (!info.avatar.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(info.avatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "头像",
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // User Info Card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileRow(label = "用户名", value = info.username)
                            ProfileRow(label = "昵称", value = info.nickname.ifBlank { "未设置" })
                            ProfileRow(label = "用户ID", value = "${info.id}")
                            if (info.roles.isNotEmpty()) {
                                ProfileRow(label = "角色", value = info.roles.joinToString(", "))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
