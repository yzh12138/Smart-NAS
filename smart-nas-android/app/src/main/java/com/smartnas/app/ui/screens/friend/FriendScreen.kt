package com.smartnas.app.ui.screens.friend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.Friend
import com.smartnas.app.data.model.FriendRequest
import com.smartnas.app.data.model.UserInfo
import com.smartnas.app.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends
    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests: StateFlow<List<FriendRequest>> = _requests
    private val _searchResults = MutableStateFlow<List<UserInfo>>(emptyList())
    val searchResults: StateFlow<List<UserInfo>> = _searchResults

    fun loadFriends() {
        viewModelScope.launch {
            try {
                val resp = api.getFriendList()
                if (resp.isSuccessful && resp.body()?.code == 0) _friends.value = resp.body()?.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            try {
                val resp = api.getPendingFriendRequests()
                if (resp.isSuccessful && resp.body()?.code == 0) _requests.value = resp.body()?.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun searchUsers(keyword: String) {
        viewModelScope.launch {
            try {
                val resp = api.searchUsers(keyword)
                if (resp.isSuccessful && resp.body()?.code == 0) _searchResults.value = resp.body()?.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun sendRequest(friendId: Long) {
        viewModelScope.launch {
            try { api.sendFriendRequest(friendId); _searchResults.value = emptyList() } catch (_: Exception) {}
        }
    }

    fun acceptRequest(id: Long) {
        viewModelScope.launch {
            try { api.acceptFriendRequest(id); loadRequests(); loadFriends() } catch (_: Exception) {}
        }
    }

    fun rejectRequest(id: Long) {
        viewModelScope.launch {
            try { api.rejectFriendRequest(id); loadRequests() } catch (_: Exception) {}
        }
    }

    fun removeFriend(friendId: Long) {
        viewModelScope.launch {
            try { api.removeFriend(friendId); loadFriends() } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(navController: NavController, viewModel: FriendViewModel = hiltViewModel()) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadFriends(); viewModel.loadRequests() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("好友管理") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("好友列表") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("请求 (${requests.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("搜索") })
            }

            when (tab) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(friends) { f ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(f.friendNickname.ifEmpty { f.friendName }, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { viewModel.removeFriend(f.friendId) }) {
                                    Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(requests) { r ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.fromNickname.ifEmpty { r.fromUsername }, style = MaterialTheme.typography.bodyMedium)
                                    Text(r.createTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.acceptRequest(r.id) }) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { viewModel.rejectRequest(r.id) }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
                2 -> Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        label = { Text("搜索用户") }, singleLine = true,
                        trailingIcon = { IconButton(onClick = { viewModel.searchUsers(searchQuery) }) { Icon(Icons.Default.Search, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchResults) { user ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(user.nickname.ifEmpty { user.username }, style = MaterialTheme.typography.bodyMedium)
                                        Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.sendRequest(user.id) }) { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) }
