package com.smartnas.app.data.repository

import com.smartnas.app.data.api.RetrofitHolder
import com.smartnas.app.data.model.LoginRequest
import com.smartnas.app.data.model.UserInfo
import com.smartnas.app.util.Resource
import com.smartnas.app.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val tokenManager: TokenManager,
    private val retrofitHolder: RetrofitHolder
) {
    val isLoggedIn: Flow<Boolean> = tokenManager.token.map { !it.isNullOrBlank() }
    val username: Flow<String?> = tokenManager.username
    val serverUrl: Flow<String?> = tokenManager.serverUrl

    suspend fun login(serverUrl: String, username: String, password: String): Resource<UserInfo> {
        return withContext(Dispatchers.IO) {
            try {
                tokenManager.saveServerUrl(serverUrl)
                tokenManager.saveUsername(username)
                retrofitHolder.rebuild("${serverUrl.trimEnd('/')}/")

                val api = retrofitHolder.currentApi
                    ?: return@withContext Resource.Error("API 初始化失败")

                val response = api.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body()?.code == 200) {
                    val loginData = response.body()!!.data
                    val token = loginData?.token ?: ""
                    tokenManager.saveToken(token)
                    val userInfo = UserInfo(
                        id = loginData?.userId ?: 0,
                        username = loginData?.username ?: username,
                        nickname = loginData?.nickname ?: username
                    )
                    Resource.Success(userInfo)
                } else {
                    Resource.Error(response.body()?.message ?: "登录失败")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    suspend fun getUserInfo(): Resource<UserInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val api = retrofitHolder.currentApi
                    ?: return@withContext Resource.Error("API 未初始化")
                val response = api.getUserInfo()
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()!!.data
                    if (data != null) {
                        Resource.Success(data)
                    } else {
                        Resource.Error("获取用户信息为空")
                    }
                } else {
                    Resource.Error(response.body()?.message ?: "获取用户信息失败")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            try { retrofitHolder.currentApi?.logout() } catch (_: Exception) {}
            tokenManager.clearAll()
        }
    }
}
