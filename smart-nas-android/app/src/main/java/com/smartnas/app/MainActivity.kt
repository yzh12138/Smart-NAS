package com.smartnas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.smartnas.app.ui.navigation.AppNavGraph
import com.smartnas.app.ui.navigation.Routes
import com.smartnas.app.ui.screens.login.LoginViewModel
import com.smartnas.app.ui.theme.SmartNASTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartNASTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val loginVm: LoginViewModel = hiltViewModel()
                    val isLoggedIn by loginVm.isLoggedIn.collectAsState(initial = false)

                    val startDest = if (isLoggedIn) Routes.HOME else Routes.LOGIN
                    AppNavGraph(navController = navController, startDestination = startDest)
                }
            }
        }
    }
}
