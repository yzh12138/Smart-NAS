package com.smartnas.app.data.api

import com.smartnas.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SmartNASApi {

    // ==================== Auth ====================
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResult<LoginResponse>>

    @GET("/api/auth/info")
    suspend fun getUserInfo(): Response<ApiResult<UserInfo>>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<ApiResult<Unit>>

    // ==================== Photo ====================
    @Multipart
    @POST("/api/photo/upload")
    suspend fun uploadPhotos(
        @Part files: List<MultipartBody.Part>,
        @Part("tags") tags: RequestBody? = null,
        @Part("city") city: RequestBody? = null,
        @Part("aiEnabled") aiEnabled: RequestBody? = null
    ): Response<ApiResult<List<Photo>>>

    @GET("/api/photo/list")
    suspend fun getPhotoList(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("tag") tag: String? = null,
        @Query("city") city: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("mediaType") mediaType: Int? = null
    ): Response<ApiResult<PageResult<Photo>>>

    @GET("/api/photo/{id}")
    suspend fun getPhotoDetail(@Path("id") id: Long): Response<ApiResult<Photo>>

    @DELETE("/api/photo/{id}")
    suspend fun deletePhoto(@Path("id") id: Long): Response<ApiResult<Unit>>

    @PUT("/api/photo/{id}/name")
    suspend fun updatePhotoName(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<ApiResult<Unit>>

    @GET("/api/photo/search")
    suspend fun searchPhotos(
        @Query("keyword") keyword: String
    ): Response<ApiResult<List<Photo>>>

    @POST("/api/photo/{id}/click")
    suspend fun trackPhotoClick(@Path("id") id: Long): Response<ApiResult<Unit>>

    @GET("/api/photo/recommended")
    suspend fun getRecommendedPhotos(): Response<ApiResult<List<Photo>>>

    @GET("/api/photo/map/cities")
    suspend fun getCityPhotoStats(): Response<ApiResult<List<CityStat>>>

    @GET("/api/photo/map/city/{city}")
    suspend fun getPhotosByCity(@Path("city") city: String): Response<ApiResult<List<Photo>>>

    @POST("/api/photo/{id}/ai-tags")
    suspend fun getAiSuggestedTags(@Path("id") id: Long): Response<ApiResult<List<String>>>

    @POST("/api/photo/{id}/confirm-tags")
    suspend fun confirmAiTags(
        @Path("id") id: Long,
        @Body body: Map<String, List<String>>
    ): Response<ApiResult<Unit>>

    // ==================== Photo Comments ====================
    @GET("/api/photo/{photoId}/comment")
    suspend fun getPhotoComments(@Path("photoId") photoId: Long): Response<ApiResult<List<PhotoComment>>>

    @POST("/api/photo/{photoId}/comment")
    suspend fun addPhotoComment(
        @Path("photoId") photoId: Long,
        @Body body: Map<String, String>
    ): Response<ApiResult<PhotoComment>>

    @DELETE("/api/photo/{photoId}/comment/{commentId}")
    suspend fun deletePhotoComment(
        @Path("photoId") photoId: Long,
        @Path("commentId") commentId: Long
    ): Response<ApiResult<Unit>>

    // ==================== Tags ====================
    @GET("/api/tag/list")
    suspend fun getTagList(): Response<ApiResult<List<Tag>>>

    @POST("/api/tag")
    suspend fun createTag(@Body body: Map<String, String>): Response<ApiResult<Tag>>

    @PUT("/api/tag/{id}")
    suspend fun updateTag(@Path("id") id: Long, @Body body: Map<String, String>): Response<ApiResult<Unit>>

    @DELETE("/api/tag/{id}")
    suspend fun deleteTag(@Path("id") id: Long): Response<ApiResult<Unit>>

    // ==================== Recycle ====================
    @GET("/api/recycle/list")
    suspend fun getRecycleList(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResult<PageResult<Photo>>>

    @POST("/api/recycle/restore/{id}")
    suspend fun restorePhoto(@Path("id") id: Long): Response<ApiResult<Unit>>

    @DELETE("/api/recycle/permanent/{id}")
    suspend fun permanentDelete(@Path("id") id: Long): Response<ApiResult<Unit>>

    @DELETE("/api/recycle/empty")
    suspend fun emptyRecycle(): Response<ApiResult<Unit>>

    // ==================== AI Chat ====================
    @GET("/api/ai-chat/conversations")
    suspend fun getConversations(): Response<ApiResult<List<Conversation>>>

    @POST("/api/ai-chat/conversation")
    suspend fun createConversation(@Body body: Map<String, String>? = null): Response<ApiResult<Conversation>>

    @GET("/api/ai-chat/conversation/{id}/messages")
    suspend fun getConversationMessages(@Path("id") id: Long): Response<ApiResult<List<ChatMessage>>>

    @POST("/api/ai-chat/conversation/{id}/send")
    suspend fun sendChatMessage(
        @Path("id") id: Long,
        @Body body: Map<String, Any?>
    ): Response<ApiResult<ChatMessage>>

    @DELETE("/api/ai-chat/conversation/{id}")
    suspend fun deleteConversation(@Path("id") id: Long): Response<ApiResult<Unit>>

    @Multipart
    @POST("/api/ai-chat/upload-image")
    suspend fun uploadChatImage(@Part image: MultipartBody.Part): Response<ApiResult<String>>

    // ==================== File Storage ====================
    @GET("/api/file/list")
    suspend fun getFileList(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("category") category: String? = null,
        @Query("keyword") keyword: String? = null
    ): Response<ApiResult<PageResult<FileStorage>>>

    @Multipart
    @POST("/api/file/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<ApiResult<FileStorage>>

    @DELETE("/api/file/{id}")
    suspend fun deleteFile(@Path("id") id: Long): Response<ApiResult<Unit>>

    @GET("/api/file/{id}/download")
    @Streaming
    suspend fun downloadFile(@Path("id") id: Long): Response<okhttp3.ResponseBody>

    // ==================== Book ====================
    @GET("/api/book/list")
    suspend fun getBookList(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("keyword") keyword: String? = null
    ): Response<ApiResult<PageResult<Book>>>

    @GET("/api/book/{id}")
    suspend fun getBookDetail(@Path("id") id: Long): Response<ApiResult<Book>>

    @Multipart
    @POST("/api/book/upload")
    suspend fun uploadBook(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody? = null,
        @Part("author") author: RequestBody? = null,
        @Part("description") description: RequestBody? = null
    ): Response<ApiResult<Book>>

    @DELETE("/api/book/{id}")
    suspend fun deleteBook(@Path("id") id: Long): Response<ApiResult<Unit>>

    // ==================== Family ====================
    @GET("/api/family/my")
    suspend fun getMyFamilies(): Response<ApiResult<List<Family>>>

    @POST("/api/family")
    suspend fun createFamily(@Body body: Map<String, String>): Response<ApiResult<Family>>

    @DELETE("/api/family/{id}")
    suspend fun dissolveFamily(@Path("id") id: Long): Response<ApiResult<Unit>>

    @GET("/api/family/{id}/members")
    suspend fun getFamilyMembers(@Path("id") id: Long): Response<ApiResult<List<FamilyMember>>>

    @GET("/api/family/{id}/pending")
    suspend fun getFamilyPending(@Path("id") id: Long): Response<ApiResult<List<FamilyMember>>>

    @POST("/api/family/member/{memberId}/approve")
    suspend fun approveMember(@Path("memberId") memberId: Long): Response<ApiResult<Unit>>

    @POST("/api/family/member/{memberId}/reject")
    suspend fun rejectMember(@Path("memberId") memberId: Long): Response<ApiResult<Unit>>

    @GET("/api/family/{id}/media")
    suspend fun getFamilyMedia(@Path("id") id: Long): Response<ApiResult<List<Photo>>>

    @POST("/api/family/{familyId}/share/{photoId}")
    suspend fun shareToFamily(
        @Path("familyId") familyId: Long,
        @Path("photoId") photoId: Long
    ): Response<ApiResult<Unit>>

    @POST("/api/family/{familyId}/batch-share")
    suspend fun batchShareToFamily(
        @Path("familyId") familyId: Long,
        @Body body: Map<String, List<Long>>
    ): Response<ApiResult<Unit>>

    @GET("/api/family/search/{code}")
    suspend fun searchFamilyByCode(@Path("code") code: String): Response<ApiResult<Family>>

    @POST("/api/family/{id}/join")
    suspend fun joinFamily(@Path("id") id: Long): Response<ApiResult<Unit>>

    // ==================== Friends ====================
    @GET("/api/friend/list")
    suspend fun getFriendList(): Response<ApiResult<List<Friend>>>

    @GET("/api/friend/search")
    suspend fun searchUsers(@Query("keyword") keyword: String): Response<ApiResult<List<UserInfo>>>

    @POST("/api/friend/send/{friendId}")
    suspend fun sendFriendRequest(@Path("friendId") friendId: Long): Response<ApiResult<Unit>>

    @GET("/api/friend/pending")
    suspend fun getPendingFriendRequests(): Response<ApiResult<List<FriendRequest>>>

    @POST("/api/friend/accept/{id}")
    suspend fun acceptFriendRequest(@Path("id") id: Long): Response<ApiResult<Unit>>

    @POST("/api/friend/reject/{id}")
    suspend fun rejectFriendRequest(@Path("id") id: Long): Response<ApiResult<Unit>>

    @DELETE("/api/friend/{friendId}")
    suspend fun removeFriend(@Path("friendId") friendId: Long): Response<ApiResult<Unit>>

    // ==================== Face ====================
    @GET("/api/face/clusters")
    suspend fun getFaceClusters(): Response<ApiResult<List<FaceCluster>>>

    @GET("/api/face/cluster/{id}/photos")
    suspend fun getFaceClusterPhotos(@Path("id") id: Long): Response<ApiResult<List<Photo>>>

    @POST("/api/face/cluster")
    suspend fun createFaceCluster(@Body body: Map<String, String>): Response<ApiResult<FaceCluster>>

    @PUT("/api/face/cluster/{id}")
    suspend fun renameFaceCluster(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<ApiResult<Unit>>

    @DELETE("/api/face/cluster/{id}")
    suspend fun deleteFaceCluster(@Path("id") id: Long): Response<ApiResult<Unit>>

    // ==================== City ====================
    @GET("/api/city/list")
    suspend fun getCityList(): Response<ApiResult<List<CityStat>>>

    @POST("/api/city")
    suspend fun createCity(@Body body: Map<String, Any>): Response<ApiResult<CityStat>>

    @DELETE("/api/city/{id}")
    suspend fun deleteCity(@Path("id") id: Long): Response<ApiRes