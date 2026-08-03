package com.smartnas.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnas.app.data.model.UserInfo
import com.smartnas.app.data.repository.AuthRepository
import com.smartnas.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isLoggedIn: Flow<Boolean> = authRepository.isLoggedIn

    private val _loginState = MutableStateFlow<Resource<UserInfo>>(Resource.Loading)
    val loginState: StateFlow<Resource<UserInfo>> = _loginState

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl

    init {
        viewModelScope.launch {
            authRepository.serverUrl.collect { url ->
                if (!url.isNullOrBlank()) _serverUrl.value = url
            }
        }
    }

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun login(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            authRepository.login(serverUrl, username, password).collect {
                _loginState.value = it
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
       