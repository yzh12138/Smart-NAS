package com.smartnas.app.di

import com.smartnas.app.data.api.RetrofitHolder
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Delegates all SmartNASApi calls to RetrofitHolder.currentApi,
 * ensuring the latest Retrofit instance is always used after rebuild().
 */
class SmartNASApiDelegate(
    private val holder: RetrofitHolder
) : SmartNASApi {

    private val api: SmartNASApi
        get() = holder.currentApi ?: throw IllegalStateException("SmartNASApi not initialized")

    // ==================== Auth ====================
    override suspend fun login(request: LoginRequest) = api.login(request)
    override suspend fun getUserInfo() = api.getUserInfo()
    override suspend fun logout() = api.logout()

    // ==================== Photo ====================
    override suspend fun uploadPhotos(
        files: List<MultipartBody.Part>,
        newTags: RequestBody?,
        city: RequestBody?,
        province: RequestBody?,
        aiTag: RequestBody?
    ) = api.uploadPhotos(files, newTags, city, province, aiTag)

    override suspend fun getPhotoList(
        page: Int, size: Int, tagId: Long?,
        city: String?, startDate: String?, endDate: String?, mediaType: String?
    ) = api.getPhotoList(page, size, tagId, city, startDate, endDate, mediaType)

    override suspend fun getPhotoDetail(id: Long) = api.getPhotoDetail(id)
    override suspend fun deletePhoto(id: Long) = api.deletePhoto(id)
    override suspend fun updatePhotoName(id: Long, body: Map<String, String>) = api.updatePhotoName(id, body)
    override suspend fun searchPhotos(keyword: String) = api.searchPhotos(keyword)
    override suspend fun trackPhotoClick(id: Long) = api.trackPhotoClick(id)
    override suspend fun getRecommendedPhotos() = api.getRecommendedPhotos()
    override suspend fun getCityPhotoStats() = api.getCityPhotoStats()
    override suspend fun getPhotosByCity(city: String) = api.getPhotosByCity(city)
    override suspend fun getAiSuggestedTags(id: Long) = api.getAiSuggestedTags(id)
    override suspend fun confirmAiTags(id: Long, body: Map<String, List<String>>) = api.confirmAiTags(id, body)

    // ==================== Photo Comments ====================
    override suspend fun getPhotoComments(photoId: Long) = api.getPhotoComments(photoId)
    override suspend fun addPhotoComment(photoId: Long, body: Map<String, String>) = api.addPhotoComment(photoId, body)
    override suspend fun deletePhotoComment(photoId: Long, commentId: Long) = api.deletePhotoComment(photoId, commentId)

    // ==================== Tags ====================
    override suspend fun getTagList() = api.getTagList()
    override suspend fun createTag(body: Map<String, String>) = api.createTag(body)
    override suspend fun updateTag(id: Long, body: Map<String, String>) = api.updateTag(id, body)
    override suspend fun deleteTag(id: Long) = api.deleteTag(id)

    // ==================== Recycle ====================
    override suspend fun getRecycleList(page: Int, size: Int) = api.getRecycleList(page, size)
    override suspend fun restorePhoto(id: Long) = api.restorePhoto(id)
    override suspend fun permanentDelete(id: Long) = api.permanentDelete(id)
    override suspend fun emptyRecycle() = api.emptyRecycle()

    // ==================== AI Chat ====================
    override suspend fun getConversations() = api.getConversations()
    override suspend fun createConversation(body: Map<String, String>?) = api.createConversation(body)
    override suspend fun getConversationMessages(id: Long) = api.getConversationMessages(id)
    override suspend fun sendChatMessage(id: Long, body: Map<String, Any?>) = api.sendChatMessage(id, body)
    override suspend fun deleteConversation(id: Long) = api.deleteConversation(id)
    override suspend fun uploadChatImage(image: MultipartBody.Part) = api.uploadChatImage(image)

    // ==================== File Storage ====================
    override suspend fun getFileList(page: Int, size: Int, category: String?, keyword: String?) = api.getFileList(page, size, category, keyword)
    override suspend fun uploadFile(file: MultipartBody.Part) = api.uploadFile(file)
    override suspend fun deleteFile(id: Long) = api.deleteFile(id)
    override suspend fun downloadFile(id: Long) = api.downloadFile(id)

    // ==================== Book ====================
    override suspend fun getBookList(page: Int, size: Int, keyword: String?) = api.getBookList(page, size, keyword)
    override suspend fun getBookDetail(id: Long) = api.getBookDetail(id)
    override suspend fun uploadBook(file: MultipartBody.Part, title: RequestBody?, author: RequestBody?, description: RequestBody?) = api.uploadBook(file, title, author, description)
    override suspend fun deleteBook(id: Long) = api.deleteBook(id)

    // ==================== Family ====================
    override suspend fun getMyFamilies() = api.getMyFamilies()
    override suspend fun createFamily(body: Map<String, String>) = api.createFamily(body)
    override suspend fun dissolveFamily(id: Long) = api.dissolveFamily(id)
    override suspend fun getFamilyMembers(id: Long) = api.getFamilyMembers(id)
    override suspend fun getFamilyPending(id: Long) = api.getFamilyPending(id)
    override suspend fun approveMember(memberId: Long) = api.approveMember(memberId)
    override suspend fun rejectMember(memberId: Long) = api.rejectMember(memberId)
    override suspend fun getFamilyMedia(id: Long) = api.getFamilyMedia(id)
    override suspend fun shareToFamily(familyId: Long, photoId: Long) = api.shareToFamily(familyId, photoId)
    override suspend fun batchShareToFamily(familyId: Long, body: Map<String, List<Long>>) = api.batchShareToFamily(familyId, body)
    override suspend fun searchFamilyByCode(code: String) = api.searchFamilyByCode(code)
    override suspend fun joinFamily(id: Long) = api.joinFamily(id)

    // ==================== Friends ====================
    override suspend fun getFriendList() = api.getFriendList()
    override suspend fun searchUsers(keyword: String) = api.searchUsers(keyword)
    override suspend fun sendFriendRequest(friendId: Long) = api.sendFriendRequest(friendId)
    override suspend fun getPendingFriendRequests() = api.getPendingFriendRequests()
    override suspend fun acceptFriendRequest(id: Long) = api.acceptFriendRequest(id)
    override suspend fun rejectFriendRequest(id: Long) = api.rejectFriendRequest(id)
    override suspend fun removeFriend(friendId: Long) = api.removeFriend(friendId)

    // ==================== Face ====================
    override suspend fun getFaceClusters() = api.getFaceClusters()
    override suspend fun getFaceClusterPhotos(id: Long) = api.getFaceClusterPhotos(id)
    override suspend fun createFaceCluster(body: Map<String, String>) = api.createFaceCluster(body)
    override suspend fun renameFaceCluster(id: Long, body: Map<String, String>) = api.renameFaceCluster(id, body)
    override suspend fun deleteFaceCluster(id: Long) = api.deleteFaceCluster(id)

    // ==================== City ====================
    override suspend fun getCityList() = api.getCityList()
    override suspend fun createCity(body: Map<String, Any>) = api.createCity(body)
    override suspend fun deleteCity(id: Long) = api.deleteCity(id)
}
