package com.smartnas.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.DashboardStats
import com.smartnas.app.data.model.Photo
import com.smartnas.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: SmartNASApi
) : ViewModel() {

    private val _stats = MutableStateFlow<Resource<DashboardStats>>(Resource.Loading)
    val stats: StateFlow<Resource<DashboardStats>> = _stats

    private val _recentPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val recentPhotos: StateFlow<List<Photo>> = _recentPhotos

    fun loadDashboard() {
        viewModelScope.launch {
            _stats.value = Resource.Loading
            try {
                val photoResp = api.getPhotoList(page = 1, size = 10, mediaType = 0)
                val videoResp = api.getPhotoList(page = 1, size = 1, mediaType = 1)
                val cityResp = api.getCityPhotoStats()

                val photos = photoResp.body()?.data?.records ?: emptyList()
                val photoCount = photoResp.body()?.data?.total?.toInt() ?: 0
                val videoCount = videoResp.body()?.data?.total?.toInt() ?: 0
                val cities = cityResp.body()?.data ?: emptyList()

                _recentPhotos.value = photos
                _stats.value = Resource.Success(
                    DashboardStats(
                        photoCount = photoCount,
                        videoCount = videoCount,
                        recentPhotos = photos,
                        cityStats = cities
                    )
                )
            } catch (e: Exception) {
                _stats.value = Resource.Error(e.message ?: 