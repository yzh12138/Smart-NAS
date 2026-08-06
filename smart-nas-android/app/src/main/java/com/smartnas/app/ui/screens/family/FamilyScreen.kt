package com.smartnas.app.ui.screens.family

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
import com.smartnas.app.data.model.Family
import com.smartnas.app.data.model.FamilyMember
import com.smartnas.app.data.model.Photo
import com.smartnas.app.ui.components.*
import com.smartnas.app.util.Resource
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(private val api: SmartNASApi) : androidx.lifecycle.ViewModel() {
    private val _families = MutableStateFlow<List<Family>>(emptyList())
    val families: StateFlow<List<Family>> = _families
    private val _members = MutableStateFlow<List<FamilyMember>>(emptyList())
    val members: StateFlow<List<FamilyMember>> = _members
    private val _media = MutableStateFlow<List<Photo>>(emptyList())
    val media: StateFlow<List<Photo>> = _media

    fun loadFamilies() {
        viewModelScope.launch {
            try {
                val resp = api.getMyFamilies()
                if (resp.isSuccessful && resp.body()?.code == 200) _families.value = resp.body()!!.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun createFamily(name: String) {
        viewModelScope.launch {
            try { api.createFamily(mapOf("name" to name)); loadFamilies() } catch (_: Exception) {}
        }
    }

    fun joinFamily(code: String) {
        viewModelScope.launch {
            try {
                val resp = api.searchFamilyByCode(code)
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    resp.body()!!.data?.let { api.joinFamily(it.id) }
                    loadFamilies()
                }
            } catch (_: Exception) {}
        }
    }

    fun loadMembers(familyId: Long) {
        viewModelScope.launch {
            try {
                val resp = api.getFamilyMembers(familyId)
                if (resp.isSuccessful && resp.body()?.code == 200) _members.value = resp.body()!!.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun loadMedia(familyId: Long) {
        viewModelScope.launch {
            try {
                val resp = api.getFamilyMedia(familyId)
                if (resp.isSuccessful && resp.body()?.code == 200) _media.value = resp.body()!!.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun dissolveFamily(id: Long) {
        viewModelScope.launch {
            try { api.dissolveFamily(id); loadFamilies() } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    navController: NavController,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val families by viewModel.families.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var familyName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var selectedFamily by remember { mutableStateOf<Family?>(null) }

    LaunchedEffect(Unit) { viewModel.loadFamilies() }

    Scaffold(
        topBar = { SmartTopBar(title = "家庭共享", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { showJoinDialog = true }) {
                    Icon(Icons.Default.Login, contentDescription = "加入家庭")
                }
                SmallFloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "创建家庭")
                }
            }
        }
    ) { padding ->
        if (families.isEmpty()) {
            EmptyState(icon = Icons.Default.People, title = "暂无家庭", subtitle = "创建或加入一个家庭", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(families) { family ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(family.name, style = MaterialTheme.typography.titleMedium)
                            Text("邀请码: ${family.inviteCode}", style = MaterialTheme.typography.bodySmall)
                            Text("${family.memberCount} 位成员", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { selectedFamily = family; viewModel.loadMembers(family.id); viewModel.loadMedia(family.id) }) {
                                    Text("查看详情")
                                }
                                TextButton(onClick = { viewModel.dissolveFamily(family.id) }) {
                                    Text("解散", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("创建家庭") },
            text = { OutlinedTextField(value = familyName, onValueChange = { familyName = it }, label = { Text("家庭名称") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.createFamily(familyName); familyName = ""; showCreateDialog = false }) { Text("创建") } },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("取消") } }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("加入家庭") },
            text = { OutlinedTextField(value = inviteCode, onValueChange = { inviteCode = it }, label = { Text("邀请码") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.joinFamily(inviteCode); inviteCode = ""; showJoinDialog = false }) { Text("加入") } },
            dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text("取消") } }
        )
    }
}
