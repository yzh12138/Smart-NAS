package com.smartnas.app.data.repository

import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.LoginRequest
import com.smartnas.app.data.model.UserInfo
import com.smartnas.app.util.Resource
import com.smartnas.app.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: SmartNASApi,
    private val tokenManager: TokenManager
) {
    val isLoggedIn: Flow<Boolean> = tokenManager.token.map { !it.isNullOrBlank() }
    val username: Flow<String?> = tokenManager.username
    val serverUrl: Flow<String?> = tokenManager.serverUrl

    suspend fun login(serverUrl: String, username: String, password: String): Flow<Resource<UserInfo>> = flow {
        emit(Resource.Loading)
        try {
            tokenManager.saveServerUrl(serverUrl)
            tokenManager.saveUsername(username)

            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body()?.code == 0) {
                val token = response.body()!!.data?.token ?: response.body()!!.message
                tokenManager.saveToken(token)
                val userInfo = response.body()!!.data?.user ?: UserInfo(username = username)
                emit(Resource.Success(userInfo))
            } else {
                emit(Resource.Error(response.body()?.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    suspend fun getUserInfo(): Flow<Resource<UserInfo>> = flow {
        try {
            val response = api.getUserInfo()
            if (response.isSuccessful && response.body()?.code == 0) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "获取用户信息失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    suspend fun logout() {
        try { api.logout() } catch (_