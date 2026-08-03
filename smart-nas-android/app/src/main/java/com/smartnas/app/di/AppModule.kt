package com.smartnas.app.di

import android.content.Context
import com.smartnas.app.data.api.RetrofitHolder
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.util.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, 50L * 1024 * 1024) // 50MB 缓存
    }

    @Provides
    @Singleton
    fun provideRetrofitHolder(tokenManager: TokenManager, cache: Cache): RetrofitHolder {
        return RetrofitHolder(tokenManager, cache)
    }

    @Provides
    @Singleton
    fun provideSmartNASApi(retrofitHolder: RetrofitHolder): SmartNASApi {
        return runBlocking {
            retrofitHolder.a