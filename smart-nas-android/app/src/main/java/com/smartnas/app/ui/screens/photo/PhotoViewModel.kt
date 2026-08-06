package com.smartnas.app.ui.screens.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.*
import com.smartnas.app.util.BaseUrlHolder
import com.smartnas.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class PhotoViewModel @Inject constructor(
    private val api: SmartNASApi,
    val baseUrlHolder: BaseUrlHolder
) : ViewModel() {

    private val _photos = MutableStateFlow<Resource<PageResult<Photo>>>(Resource.Loading)
    val photos: StateFlow<Resource<PageResult<Photo>>> = _photos

    private val _photoDetail = MutableStateFlow<Resource<Photo>>(Resource.Loading)
    val photoDetail: StateFlow<Resource<Photo>> = _photoDetail

    private val _comments = MutableStateFlow<List<PhotoComment>>(emptyList())
    val comments: StateFlow<List<PhotoComment>> = _comments

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags

    private val _uploadState = MutableStateFlow<Resource<List<Photo>>>(Resource.Loading)
    val uploadState: StateFlow<Resource<List<Photo>>> = _uploadState

    private var currentPage = 1
    private var currentTagId: Long? = null

    fun loadPhotos(page: Int = 1, tagId: Long? = null) {
        currentPage = page
        currentTagId = tagId
        viewModelScope.launch {
            _photos.value = Resource.Loading
            try {
                val resp = api.getPhotoList(page = page, size = 20, tagId = tagId, mediaType = "0")
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _photos.value = Resource.Success(resp.body()!!.data!!)
                } else {
                    _photos.value = Resource.Error(resp.body()?.message ?: "加载失败")
                }
            } catch (e: Exception) {
                _photos.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun loadPhotoDetail(id: Long) {
        viewModelScope.launch {
            _photoDetail.value = Resource.Loading
            try {
                val resp = api.getPhotoDetail(id)
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _photoDetail.value = Resource.Success(resp.body()!!.data!!)
                } else {
                    _photoDetail.value = Resource.Error(resp.body()?.message ?: "加载失败")
                }
            } catch (e: Exception) {
                _photoDetail.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun loadComments(photoId: Long) {
        viewModelScope.launch {
            try {
                val resp = api.getPhotoComments(photoId)
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _comments.value = resp.body()!!.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun addComment(photoId: Long, content: String) {
        viewModelScope.launch {
            try {
                api.addPhotoComment(photoId, mapOf("content" to content))
                loadComments(photoId)
            } catch (_: Exception) {}
        }
    }

    fun deletePhoto(id: Long) {
        viewModelScope.launch {
            try {
                api.deletePhoto(id)
            } catch (_: Exception) {}
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            try {
                val resp = api.getTagList()
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _tags.value = resp.body()!!.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun uploadPhotos(files: List<MultipartBody.Part>, tags: String?, city: String?) {
        viewModelScope.launch {
            _uploadState.value = Resource.Loading
            try {
                val tagsBody = tags?.toRequestBody("text/plain".toMediaTypeOrNull())
                val cityBody = city?.toRequestBody("text/plain".toMediaTypeOrNull())
                val resp = api.uploadPhotos(files, newTags = tagsBody, city = cityBody)
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _uploadState.value = Resource.Success(resp.body()!!.data ?: emptyList())
                } else {
                    _uploadState.value = Resource.Error(resp.body()?.message ?: "上传失败")
                }
            } catch (e: Exception) {
                _uploadState.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun searchPhotos(keyword: String) {
        viewModelScope.launch {
            _photos.value = Resource.Loading
            try {
                val resp = api.searchPhotos(keyword)
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    val list = resp.body()!!.data ?: emptyList()
                    _photos.value = Resource.Success(PageResult(records = list, total = list.size.toLong()))
                } else {
                    _photos.value = Resource.Error(resp.body()?.message ?: "搜索失败")
                }
            } catch (e: Exception) {
                _photos.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun refresh() {
        loadPhotos(currentPage, currentTagId)
    }
}
