package com.smartnas.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.smartnas.app.ui.components.SmartTopBar
import com.smartnas.app.ui.navigation.Routes
import com.smartnas.app.ui.screens.login.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SmartTopBar(title = "设置", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Settings Items
            SettingsItem(
                icon = Icons.Default.Person,
                title = "个人资料",
                subtitle = "查看和修改个人信息",
                onClick = { navController.navigate(Routes.PROFILE) }
            )
            SettingsItem(
                icon = Icons.Default.People,
                title = "好友管理",
                subtitle = "添加和管理好友",
                onClick = { navController.navigate(Routes.FRIEND) }
            )
            SettingsItem(
                icon = Icons.Default.Face,
                title = "人脸识别",
                subtitle = "查看人脸聚类",
                onClick = { navController.navigate(Routes.FACE) }
            )
            SettingsItem(
                icon = Icons.Default.Label,
                title = "标签管理",
                subtitle = "管理照片标签",
                onClick = { navController.navigate(Routes.TAGS) }
            )
            SettingsItem(
                icon = Icons.Default.Delete,
                title = "回收站",
                subtitle = "恢复已删除的照片",
                onClick = { navController.navigate(Routes.RECYCLE) }
            )
            SettingsItem(
                icon = Icons.Default.Book,
                title = "图书管理",
                subtitle = "电子书管理",
                onClick = { navController.navigate(Routes.BOOK) }
            )
            SettingsItem(
                icon = Icons.Default.PeopleOutline,
                title = "家庭共享",
                subtitle = "家庭照片共享",
                onClick = { navController.navigate(Routes.FAMILY) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("退出登录")
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    loginViewModel.logout()
                    showLogoutDialog = false
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }) { Text("确定", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
