import Foundation

// MARK: - API Response
struct ApiResult<T: Codable>: Codable {
    let code: Int
    let message: String
    let data: T?
}

struct PageResult<T: Codable>: Codable {
    let records: [T]
    let total: Int
    let size: Int
    let current: Int
    let pages: Int
}

// MARK: - Auth
struct LoginRequest: Codable {
    let username: String
    let password: String
}

struct LoginResponse: Codable {
    let token: String
    let userId: Int?
    let username: String?
    let nickname: String?
}

struct UserInfo: Codable, Identifiable {
    let userId: Int
    let username: String
    let nickname: String
    let avatar: String?
    let familyRole: String?

    var id: Int { userId }

    init(userId: Int = 0, username: String = "", nickname: String = "", avatar: String? = nil, familyRole: String? = nil) {
        self.userId = userId
        self.username = username
        self.nickname = nickname
        self.avatar = avatar
        self.familyRole = familyRole
    }
}

// MARK: - Photo
struct Photo: Codable, Identifiable {
    let id: Int
    let name: String
    let fileName: String
    let filePath: String
    let thumbnailPath: String?
    let mimeType: String
    let fileSize: Int64
    let mediaType: Int // 0=photo, 1=video
    let width: Int
    let height: Int
    let duration: Int
    let latitude: Double?
    let longitude: Double?
    let city: String?
    let province: String?
    let shootTime: String?
    let uploadTime: String
    let clickCount: Int
    let tags: [Tag]
    let aiTags: String?
    let aiAnalyzed: Bool
    let userId: Int
    let username: String
}

struct Tag: Codable, Identifiable {
    let id: Int
    let name: String
    let color: String

    init(id: Int = 0, name: String = "", color: String = "#409EFF") {
        self.id = id
        self.name = name
        self.color = color
    }
}

struct CityStat: Codable, Identifiable {
    var id: String { city }
    let city: String
    let count: Int
    let latitude: Double?
    let longitude: Double?
}

struct PhotoComment: Codable, Identifiable {
    let id: Int
    let photoId: Int
    let userId: Int
    let username: String
    let nickname: String
    let content: String
    let createTime: String
}

// MARK: - AI Chat
struct Conversation: Codable, Identifiable {
    let id: Int
    let title: String
    let createTime: String
    let updateTime: String
}

struct ChatMessage: Codable, Identifiable {
    let id: Int
    let conversationId: Int
    let role: String // user / assistant
    let content: String
    let imageUrl: String?
    let createTime: String

    init(id: Int = 0, conversationId: Int = 0, role: String = "", content: String = "", imageUrl: String? = nil, createTime: String = "") {
        self.id = id
        self.conversationId = conversationId
        self.role = role
        self.content = content
        self.imageUrl = imageUrl
        self.createTime = createTime
    }
}

// MARK: - File Storage
struct FileStorage: Codable, Identifiable {
    let id: Int
    let fileName: String
    let originalName: String
    let filePath: String
    let fileSize: Int64
    let mimeType: String
    let category: String
    let uploadTime: String
    let userId: Int
}

// MARK: - Book
struct Book: Codable, Identifiable {
    let id: Int
    let title: String
    let author: String
    let coverPath: String?
    let filePath: String
    let fileType: String
    let fileSize: Int64
    let description: String
    let uploadTime: String
    let userId: Int
}

// MARK: - Family
struct Family: Codable, Identifiable {
    let id: Int
    let familyName: String
    let familyCode: String
    let ownerId: Int
    let description: String?
    let status: Int
    let createTime: String

    var name: String { familyName }
    var inviteCode: String { familyCode }
}

struct FamilyMember: Codable, Identifiable {
    let id: Int
    let familyId: Int
    let userId: Int
    let username: String
    let nickname: String
    let status: Int // 0=pending, 1=approved
    let joinTime: String
}

// MARK: - Friend
struct Friend: Codable, Identifiable {
    let id: Int
    let friendId: Int
    let friendName: String
    let friendNickname: String
    let avatar: String?
    let status: Int
    let createTime: String
}

struct FriendRequest: Codable, Identifiable {
    let id: Int
    let fromUserId: Int
    let fromUsername: String
    let fromNickname: String
    let toUserId: Int
    let status: Int
    let createTime: String
}

// MARK: - Face
struct FaceCluster: Codable, Identifiable {
    let id: Int
    let name: String
    let photoCount: Int
    let coverPhotoId: Int?
    let userId: Int
    let createTime: String
}

// MARK: - Helpers
func formatFileSize(_ bytes: Int64) -> String {
    switch bytes {
    case ..<1024: return "\(bytes) B"
    case ..<1024*1024: return String(format: "%.1f KB", Double(bytes) / 1024.0)
    case ..<1024*1024*1024: return String(format: "%.1f MB", Double(bytes) / (1024.0 * 1024.0))
    default: return String(format: "%.2f GB", Double(bytes) / (1024.0 * 1024.0 * 1024.0))
    }
}