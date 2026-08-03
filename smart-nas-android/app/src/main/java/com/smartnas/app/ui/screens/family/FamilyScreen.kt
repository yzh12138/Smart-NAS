package com.smartnas.app.ui.screens.family

import androidx.compose.foundation.clickable
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
import com.smartnas.app.data.model.Family
import com.smartnas.app.data.model.FamilyMember
import com.smartnas.app.data.model.Photo
import com.smartnas.app.ui.components.*
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
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _selectedFamily = MutableStateFlow<Family?>(null)
    val selectedFamily: StateFlow<Family?> = _selectedFamily

    fun loadFamilies() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = api.getMyFamilies()
                if (resp.isSuccessful && resp.body()?.code == 0) _families.value = resp.body()?.data ?: emptyList()
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun createFamily(name: String) {
        viewModelScope.launch {
            try { api.createFamily(mapOf("name" to name)); loadFamilies() } catch (_: Exception) {}
        }
    }

    fun selectFamily(family: Family) {
        _selectedFamily.value = family
        loadMembers(family.id)
        loadMedia(family.id)
    }

    fun loadMembers(familyId: Long) {
        viewModelScope.launch {
            try {
                val resp = api.getFamilyMembers(familyId)
                if (resp.isSuccessful && resp.body()?.code == 0) _members.value = resp.body()?.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun loadMedia(familyId: Long) {
        viewModelScope.launch {
            try {
                val resp = api.getFamilyMedia(familyId)
                if (resp.isSuccessful && resp.body()?.code == 0) _media.value = resp.body()?.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun joinFamily(code: String) {
        viewModelScope.launch {
            try {
                val search = api.searchFamilyByCode(code)
                if (search.isSuccessful && search.body()?.data != null) {
                    api.joinFamily(search.body()!!.data!!.id)
                    loadFamilies()
                }
            } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(navController: NavController, viewModel: FamilyViewModel = hiltViewModel()) {
    val families by viewModel.families.collectAsStateWithLifecycle()
    val selectedFamily by viewModel.selectedFamily.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var familyName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var currentTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.loadFamilies() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFamily?.name ?: "家庭共享") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedFamily != null) viewModel.selectFamily(null as Family) else navController.popBackStack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    if (selectedFamily == null) {
                        IconButton(onClick = { showJoinDialog = true }) { Icon(Icons.Default.GroupAdd, null) }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedFamily == null) {
                FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, null) }
            }
        }
    ) { padding ->
        if (selectedFamily == null) {
            // Family List
            when {
                isLoading -> LoadingScreen(modifier = Modifier.padding(padding))
                families.isEmpty() -> EmptyState(Icons.Default.FamilyRestroom, "暂无家庭", "创建或加入一个家庭", modifier = Modifier.padding(padding))
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(families) { family ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.selectFamily(family) }
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(family.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${family.memberCount} 位成员 · 邀请码: ${family.inviteCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Family Detail
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                TabRow(selectedTabIndex = currentTab) {
                    Tab(selected = currentTab == 0, onClick = { currentTab = 0 }, text = { Text("成员") }, icon = { Icon(Icons.Default.People, null, Modifier.size(18.dp)) })
                    Tab(selected = currentTab == 1, onClick = { currentTab = 1 }, text = { Text("共享照片") }, icon = { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp)) })
                }
                when (currentTab) {
                    0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(members) { m ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(m.nickname.ifEmpty { m.username }, style = MaterialTheme.typography.bodyMedium)
                                        Text(if (m.status == 1) "已加入" else "待审核", style = MaterialTheme.typography.bodySmall, color = if (m.status == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    1 -> if (media.isEmpty()) EmptyState(Icons.Default.Photo, "暂无共享照片")
                    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(media) { photo ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Photo, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column { Text(photo.name, maxLines = 1) }
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
                text = { OutlinedTextField(familyName, { familyName = it }, label = { Text("家庭名称") }, singleLine = true) },
                confirmButton = { TextButton(onClick = { viewModel.createFamily(familyName); familyName = ""; showCreateDialog = false }) { Text("创建") } },
                dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("取消") } }
            )
        }

        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                title = { Text("加入家庭") },
                text = { OutlinedTextField(inviteCode, { inviteCode = it }, label = { Text("邀请码") }, singleLine = true) },
                confirmButton = { TextButton(onClick = { viewModel.joinFamily