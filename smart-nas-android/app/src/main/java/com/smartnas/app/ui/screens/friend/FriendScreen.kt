package com.smartnas.app.ui.screens.friend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.Friend
import com.smartnas.app.data.model.FriendRequest
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
                if (resp.isSuccessful && resp.body()?.code == 200) _friends.value = resp.body()!!.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            try {
                val resp = api.getPendingFriendRequests()
                if (resp.isSuccessful && resp.body()?.code == 200) _requests.value = resp.body()!!.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun searchUsers(keyword: String) {
        viewModelScope.launch {
            try {
                val resp = api.searchUsers(keyword)
                if (resp.isSuccessful && resp.body()?.code == 200) _searchResults.value = resp.body()!!.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun sendRequest(friendId: Long) {
        viewModelScope.launch {
            try { api.sendFriendRequest(friendId) } catch (_: Exception) {}
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
fun FriendScreen(
    navController: NavController,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var searchKeyword by remember { mutableStateOf("") }
    var tabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.loadFriends(); viewModel.loadRequests() }

    Scaffold(
        topBar = { SmartTopBar(title = "好友管理", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("好友列表") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("请求 (${requests.size})") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("搜索") })
            }
            when (tabIndex) {
                0 -> LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(friends) { friend ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(friend.friendNickname.ifBlank { friend.friendName }, style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = { viewModel.removeFriend(friend.friendId) }) {
                                    Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                1 -> LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(requests) { req ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(req.fromNickname.ifBlank { req.fromUsername }, modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(onClick = { viewModel.acceptRequest(req.id) }) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    IconButton(onClick = { viewModel.rejectRequest(req.id) }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
                2 -> Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = searchKeyword, onValueChange = { searchKeyword = it }, label = { Text("搜索用户") }, singleLine = true, modifier = Modifier.weight(1f))
                        Button(onClick = { viewModel.searchUsers(searchKeyword) }) { Text("搜索") }
                    }
                    searchResults.forEach { user ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(user.nickname.ifBlank { user.username }, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.sendRequest(user.id) }) { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }
                }
            }
        }
    }
}
