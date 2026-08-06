import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case networkError(Error)
    case decodingError(Error)
    case serverError(String, Int?)
    case unauthorized

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "无效的 URL"
        case .networkError(let e): return e.localizedDescription
        case .decodingError: return "数据解析失败"
        case .serverError(let msg, _): return msg
        case .unauthorized: return "登录已过期，请重新登录"
        }
    }
}

class APIService: ObservableObject {
    static let shared = APIService()

    private var baseURL: String {
        AppState().baseURL // Fallback; should use injected state
    }

    private func request<T: Decodable>(
        method: String,
        path: String,
        body: Encodable? = nil,
        queryItems: [URLQueryItem]? = nil,
        timeout: TimeInterval = 30,
        token: String?
    ) async throws -> T {
        var components = URLComponents(string: "\(AppState().baseURL)\(path)")!
        if let queryItems = queryItems { components.queryItems = queryItems }
        guard let url = components.url else { throw APIError.invalidURL }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = method
        urlRequest.timeoutInterval = timeout
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = token {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            urlRequest.httpBody = try JSONEncoder().encode(body)
        }

        let (data, response) = try await URLSession.shared.data(for: urlRequest)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.serverError("无效响应", nil)
        }

        if httpResponse.statusCode == 401 {
            throw APIError.unauthorized
        }

        let decoder = JSONDecoder()
        do {
            let result = try decoder.decode(T.self, from: data)
            return result
        } catch {
            throw APIError.decodingError(error)
        }
    }

    // MARK: - Auth
    func login(serverURL: String, username: String, password: String) async throws -> LoginResponse {
        let components = URLComponents(string: "\(serverURL)/api/auth/login")!
        guard let url = components.url else { throw APIError.invalidURL }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.httpBody = try JSONEncoder().encode(LoginRequest(username: username, password: password))

        let (data, _) = try await URLSession.shared.data(for: urlRequest)
        let result = try JSONDecoder().decode(ApiResult<LoginResponse>.self, from: data)
        guard result.code == 200, let loginData = result.data else {
            throw APIError.serverError(result.message, result.code)
        }
        return loginData
    }

    func getUserInfo(token: String) async throws -> UserInfo {
        try await request(method: "GET", path: "/api/auth/info", token: token)
    }

    // MARK: - Photo
    func getPhotoList(token: String, page: Int = 1, size: Int = 20, tagId: Int? = nil, keyword: String? = nil, mediaType: Int? = nil) async throws -> PageResult<Photo> {
        var items = [URLQueryItem(name: "page", value: "\(page)"), URLQueryItem(name: "size", value: "\(size)")]
        if let tagId = tagId { items.append(URLQueryItem(name: "tagId", value: "\(tagId)")) }
        if let keyword = keyword { items.append(URLQueryItem(name: "keyword", value: keyword)) }
        if let mediaType = mediaType { items.append(URLQueryItem(name: "mediaType", value: "\(mediaType)")) }
        let result: ApiResult<PageResult<Photo>> = try await request(method: "GET", path: "/api/photo/list", queryItems: items, token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func getPhotoDetail(token: String, id: Int) async throws -> Photo {
        let result: ApiResult<Photo> = try await request(method: "GET", path: "/api/photo/\(id)", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func deletePhoto(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/photo/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func searchPhotos(token: String, keyword: String) async throws -> [Photo] {
        let result: ApiResult<[Photo]> = try await request(method: "GET", path: "/api/photo/search", queryItems: [URLQueryItem(name: "keyword", value: keyword)], token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func trackPhotoClick(token: String, id: Int) async throws {
        let _: ApiResult<String> = try await request(method: "POST", path: "/api/photo/\(id)/click", token: token)
    }

    func getCityPhotoStats(token: String) async throws -> [CityStat] {
        let result: ApiResult<[CityStat]> = try await request(method: "GET", path: "/api/photo/map/cities", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    // MARK: - Photo Upload
    func uploadPhotos(token: String, files: [(Data, String, String)], tags: String? = nil, city: String? = nil) async throws -> [Photo] {
        let appState = AppState()
        let url = URL(string: "\(appState.baseURL)/api/photo/upload")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 300
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let boundary = UUID().uuidString
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        for (fileData, fileName, mimeType) in files {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"files\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
            body.append(fileData)
            body.append("\r\n".data(using: .utf8)!)
        }
        if let tags = tags {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"tags\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(tags)\r\n".data(using: .utf8)!)
        }
        if let city = city {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"city\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(city)\r\n".data(using: .utf8)!)
        }
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body

        let (data, _) = try await URLSession.shared.data(for: request)
        let result = try JSONDecoder().decode(ApiResult<[Photo]>.self, from: data)
        guard result.code == 200, let photos = result.data else { throw APIError.serverError(result.message, result.code) }
        return photos
    }

    // MARK: - Comments
    func getPhotoComments(token: String, photoId: Int) async throws -> [PhotoComment] {
        let result: ApiResult<[PhotoComment]> = try await request(method: "GET", path: "/api/photo/\(photoId)/comment", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func addPhotoComment(token: String, photoId: Int, content: String) async throws -> PhotoComment {
        let result: ApiResult<PhotoComment> = try await request(method: "POST", path: "/api/photo/\(photoId)/comment", body: ["content": content], token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func deletePhotoComment(token: String, photoId: Int, commentId: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/photo/\(photoId)/comment/\(commentId)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    // MARK: - Tags
    func getTagList(token: String) async throws -> [Tag] {
        let result: ApiResult<[Tag]> = try await request(method: "GET", path: "/api/tag/list", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func createTag(token: String, name: String, color: String = "#409EFF") async throws -> Tag {
        struct TagBody: Encodable { let name: String; let color: String }
        let result: ApiResult<Tag> = try await request(method: "POST", path: "/api/tag", body: TagBody(name: name, color: color), token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
        return result.data ?? Tag(id: 0, name: name, color: color)
    }

    func deleteTag(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/tag/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    // MARK: - Recycle
    func getRecycleList(token: String) async throws -> PageResult<Photo> {
        let result: ApiResult<PageResult<Photo>> = try await request(method: "GET", path: "/api/recycle/list", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func restorePhoto(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "POST", path: "/api/recycle/restore/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func permanentDelete(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/recycle/permanent/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func emptyRecycle(token: String) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/recycle/empty", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    // MARK: - AI Chat
    func getConversations(token: String) async throws -> [Conversation] {
        let result: ApiResult<[Conversation]> = try await request(method: "GET", path: "/api/ai-chat/conversations", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func createConversation(token: String, title: String = "新对话") async throws -> Conversation {
        let result: ApiResult<Conversation> = try await request(method: "POST", path: "/api/ai-chat/conversation", body: ["title": title], token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func getConversationMessages(token: String, id: Int) async throws -> [ChatMessage] {
        let result: ApiResult<[ChatMessage]> = try await request(method: "GET", path: "/api/ai-chat/conversation/\(id)/messages", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func sendChatMessage(token: String, conversationId: Int, content: String, imageUrl: String? = nil) async throws -> ChatMessage {
        struct ChatBody: Encodable {
            let content: String
            let imageUrl: String?
        }
        let body = ChatBody(content: content, imageUrl: imageUrl)
        let result: ApiResult<ChatMessage> = try await request(method: "POST", path: "/api/ai-chat/conversation/\(conversationId)/send", body: body, token: token, timeout: 180)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func deleteConversation(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/ai-chat/conversation/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    // MARK: - File Storage
    func getFileList(token: String, page: Int = 1, size: Int = 20) async throws -> PageResult<FileStorage> {
        let result: ApiResult<PageResult<FileStorage>> = try await request(method: "GET", path: "/api/file/list", queryItems: [URLQueryItem(name: "page", value: "\(page)"), URLQueryItem(name: "size", value: "\(size)")], token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func deleteFile(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/file/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func uploadFile(token: String, fileData: Data, fileName: String) async throws -> FileStorage {
        let appState = AppState()
        let url = URL(string: "\(appState.baseURL)/api/file/upload")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 120
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let boundary = UUID().uuidString
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: application/octet-stream\r\n\r\n".data(using: .utf8)!)
        body.append(fileData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body

        let (data, _) = try await URLSession.shared.data(for: request)
        let result = try JSONDecoder().decode(ApiResult<FileStorage>.self, from: data)
        guard result.code == 200, let file = result.data else { throw APIError.serverError(result.message, result.code) }
        return file
    }

    // MARK: - Book
    func getBookList(token: String) async throws -> PageResult<Book> {
        let result: ApiResult<PageResult<Book>> = try await request(method: "GET", path: "/api/book/list", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func deleteBook(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/book/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    // MARK: - Family
    func getMyFamilies(token: String) async throws -> [Family] {
        let result: ApiResult<[Family]> = try await request(method: "GET", path: "/api/family/my", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func createFamily(token: String, name: String) async throws -> Family {
        let result: ApiResult<Family> = try await request(method: "POST", path: "/api/family", body: ["name": name], token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func getFamilyMembers(token: String, familyId: Int) async throws -> [FamilyMember] {
        let result: ApiResult<[FamilyMember]> = try await request(method: "GET", path: "/api/family/\(familyId)/members", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func getFamilyMedia(token: String, familyId: Int) async throws -> [Photo] {
        let result: ApiResult<[Photo]> = try await request(method: "GET", path: "/api/family/\(familyId)/media", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func searchFamilyByCode(token: String, code: String) async throws -> Family {
        let result: ApiResult<Family> = try await request(method: "GET", path: "/api/family/search/\(code)", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func joinFamily(token: String, familyId: Int) async throws {
        let result: ApiResult<String> = try await request(method: "POST", path: "/api/family/\(familyId)/join", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func approveMember(token: String, memberId: Int) async throws {
        let result: ApiResult<String> = try await request(method: "POST", path: "/api/family/member/\(memberId)/approve", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func rejectMember(token: String, memberId: Int) async throws {
        let result: ApiResult<String> = try await request(method: "POST", path: "/api/family/member/\(memberId)/reject", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    // MARK: - Friends
    func getFriendList(token: String) async throws -> [Friend] {
        let result: ApiResult<[Friend]> = try await request(method: "GET", path: "/api/friend/list", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func searchUsers(token: String, keyword: String) async throws -> [UserInfo] {
        let result: ApiResult<[UserInfo]> = try await request(method: "GET", path: "/api/friend/search", queryItems: [URLQueryItem(name: "keyword", value: keyword)], token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func sendFriendRequest(token: String, friendId: Int) async throws {
        let result: ApiResult<String> = try await request(method: "POST", path: "/api/friend/send/\(friendId)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func getPendingFriendRequests(token: String) async throws -> [FriendRequest] {
        let result: ApiResult<[FriendRequest]> = try await request(method: "GET", path: "/api/friend/pending", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func acceptFriendRequest(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "POST", path: "/api/friend/accept/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func rejectFriendRequest(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "POST", path: "/api/friend/reject/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func removeFriend(token: String, friendId: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/friend/\(friendId)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    // MARK: - Face
    func getFaceClusters(token: String) async throws -> [FaceCluster] {
        let result: ApiResult<[FaceCluster]> = try await request(method: "GET", path: "/api/face/clusters", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func getFaceClusterPhotos(token: String, clusterId: Int) async throws -> [Photo] {
        let result: ApiResult<[Photo]> = try await request(method: "GET", path: "/api/face/cluster/\(clusterId)/photos", token: token)
        guard result.code == 200, let data = result.data else { throw APIError.serverError(result.message, result.code) }
        return data
    }

    func renameFaceCluster(token: String, id: Int, name: String) async throws {
        let result: ApiResult<String> = try await request(method: "PUT", path: "/api/face/cluster/\(id)", body: ["name": name], token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    func deleteFaceCluster(token: String, id: Int) async throws {
        let result: ApiResult<String> = try await request(method: "DELETE", path: "/api/face/cluster/\(id)", token: token)
        guard result.code == 200 else { throw APIError.serverError(result.message, result.code) }
    }

    // MARK: - City
    func getCityList(token: String) async throws -> [CityStat] {
        let result: ApiResult<[CityStat]> = try await reques