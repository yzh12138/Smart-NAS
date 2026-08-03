package com.smartnas.app.ui.screens.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.Photo
import com.smartnas.app.data.model.PhotoComment
import com.smartnas.app.data.model.Tag
import com.smartnas.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PhotoViewModel @Inject constructor(
    private val api: SmartNASApi
) : ViewModel() {

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos

    private val _photoDetail = MutableStateFlow<Resource<Photo>>(Resource.Loading)
    val photoDetail: StateFlow<Resource<Photo>> = _photoDetail

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags

    private val _comments = MutableStateFlow<List<PhotoComment>>(emptyList())
    val comments: StateFlow<List<PhotoComment>> = _comments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _uploadState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val uploadState: StateFlow<Resource<String>> = _uploadState

    fun loadPhotos(
        page: Int = 1,
        tag: String? = null,
        city: String? = null,
        keyword: String? = null,
        mediaType: Int? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getPhotoList(page = page, tag = tag, city = city, keyword = keyword, mediaType = mediaType)
                if (response.isSuccessful && response.body()?.code == 0) {
                    _photos.value = response.body()?.data?.records ?: emptyList()
                }
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            try {
                val response = api.getTagList()
                if (response.isSuccessful && response.body()?.code == 0) {
                    _tags.value = response.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun getPhotoDetail(id: Long) {
        viewModelScope.launch {
            _photoDetail.value = Resource.Loading
            try {
                val response = api.getPhotoDetail(id)
                if (response.isSuccessful && response.body()?.code == 0) {
                    _photoDetail.value = Resource.Success(response.body()!!.data!!)
                } else {
                    _photoDetail.value = Resource.Error(response.body()?.message ?: "加载失败")
                }
            } catch (e: Exception) {
                _photoDetail.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }

    fun loadComments(photoId: Long) {
        viewModelScope.launch {
            try {
                val response = api.getPhotoComments(photoId)
                if (response.isSuccessful && response.body()?.code == 0) {
                    _comments.value = response.body()?.data ?: emptyList()
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

    fun deletePhoto(id: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deletePhoto(id)
                onResult(response.isSuccessful && response.body()?.code == 0)
            } catch (_: Exception) { onResult(false) }
        }
    }

    fun searchPhotos(keyword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.searchPhotos(keyword)
                if (response.isSuccessful && response.body()?.code == 0) {
                    _photos.value = response.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun uploadPhotos(files: List<File>, tags: String? = null, city: String? = null) {
        viewModelScope.launch {
            _uploadState.value = Resource.Loading
            try {
                val parts = files.map { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("files", file.name, requestFile)
                }
                val tagsBody = tags?.toRequestBody("text/plain".toMediaTypeOrNull())
                val cityBody = city?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.uploadPhotos(parts, tags = tagsBody, city = cityBody)
                if (response.isSuccessful && response.body()?.code == 0) {
                    _uploadState.value = Resource.Success("上传成功，共 ${files.size} 个文件")
                } else {
                    _uploadState.value = Resource.Error(response.body()?.message ?: "上传失败")
                }
            } catch (e: Exception) {
                _uploadState.value = Resource.Error(e.message ?: "上传失败")
            }
        }
    