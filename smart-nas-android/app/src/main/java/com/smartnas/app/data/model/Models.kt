package com.smartnas.app.data.model

import com.google.gson.annotations.SerializedName

// ========== Auth ==========
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val user: UserInfo? = null)
data class UserInfo(
    val id: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null,
    val status: Int = 1,
    val roles: List<String> = emptyList()
)

// ========== Common ==========
data class PageResult<T>(
    val records: List<T> = emptyList(),
    val total: Long = 0,
    val size: Int = 20,
    val current: Int = 1,
    val pages: Int = 0
)

data class ApiResult<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null
)

// ========== Photo ==========
data class Photo(
    val id: Long = 0,
    val name: String = "",
    val fileName: String = "",
    val filePath: String = "",
    val thumbnailPath: String? = null,
    val mimeType: String = "",
    val fileSize: Long = 0,
    val mediaType: Int = 0, // 0=photo, 1=video
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    val province: String? = null,
    val shootTime: String? = null,
    val uploadTime: String = "",
    val clickCount: Int = 0,
    val tags: List<Tag> = emptyList(),
    val aiTags: String? = null,
    val aiAnalyzed: Boolean = false,
    val userId: Long = 0,
    val username: String = ""
)

data class Tag(
    val id: Long = 0,
    val name: String = "",
    val color: String = "#409EFF"
)

data class CityStat(
    val city: String = "",
    val count: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class PhotoComment(
    val id: Long = 0,
    val photoId: Long = 0,
    val userId: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val content: String = "",
    val createTime: String = ""
)

// ========== AI Chat ==========
data class Conversation(
    val id: Long = 0,
    val title: String = "",
    val createTime: String = "",
    val updateTime: String = ""
)

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long = 0,
    val role: String = "", // user / assistant
    val content: String = "",
    val imageUrl: String? = null,
    val createTime: String = ""
)

// ========== File Storage ==========
data class FileStorage(
    val id: Long = 0,
    val fileName: String = "",
    val originalName: String = "",
    val filePath: String = "",
    val fileSize: Long = 0,
    val mimeType: String = "",
    val category: String = "",
    val uploadTime: String = "",
    val userId: Long = 0
)

// ========== Book ==========
data class Book(
    val id: Long = 0,
    val title: String = "",
    val author: String = "",
    val coverPath: String? = null,
    val filePath: String = "",
    val fileType: String = "", // epub, pdf
    val fileSize: Long = 0,
    val description: String = "",
    val uploadTime: String = "",
    val userId: Long = 0
)

// ========== Family ==========
data class Family(
    val id: Long = 0,
    val name: String = "",
    val inviteCode: String = "",
    val ownerId: Long = 0,
    val ownerName: String = "",
    val memberCount: Int = 0,
    val createTime: String = ""
)

data class FamilyMember(
    val id: Long = 0,
    val familyId: Long = 0,
    val userId: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val status: Int = 0, // 0=pending, 1=approved
    val joinTime: String = ""
)

// ========== Friend ==========
data class Friend(
    val id: Long = 0,
    val friendId: Long = 0,
    val friendName: String = "",
    val friendNickname: String = "",
    val avatar: String? = null,
    val status: Int = 1,
    val createTime: String = ""
)

data class FriendRequest(
    val id: Long = 0,
    val fromUserId: Long = 0,
    val fromUsername: String = "",
    val fromNickname: String = "",
    val toUserId: Long = 0,
    val status: Int = 0, // 0=pending, 1=accepted, 2=rejected
    val createTime: String = ""
)

// ========== Face ==========
data class FaceCluster(
    val id: Long = 0,
    val name: String = "",
    val photoCount: Int = 0,
    val coverPhotoId: Long? = null,
    val userId: Long = 0,
    val createTime: String = ""
)

// ========== System ==========
data class DashboardStats(
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val userCount: Int = 0,
    val familyCount: Int = 0,
    val storageUsed: Long = 0,
    val storageTotal: Long = 0,
    val recentPhotos: List<Photo> = emptyList(),
    val cityStats: List<CityStat> = emptyList()
)

data class AiModelConf