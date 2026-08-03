import SwiftUI

@main
struct SmartNASApp: App {
    @StateObject private var appState = AppState()

    init() {
        // 配置全局 URL 缓存（50MB 内存 + 200MB 磁盘）
        let memoryCapacity = 50 * 1024 * 1024
        let diskCapacity = 200 * 1024 * 1024
        let cache = URLCache(memoryCapacity: memoryCapacity, diskCapacity: diskCapacity, diskPath: "SmartNAS_HTTP")
        URLCache.shared = cache
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(appState)
        }
    }
}

class AppState: ObservableObject {
    @Published var isLoggedIn: Bool = false
    @Published var serverURL: String = UserDefaults.standard.string(forKey: "server_url") ?? "http://10.0.2.2:8080"
    @Published var token: String? = KeychainHelper.get("jwt_token")
    @Published var username: String? = UserDefaults.standard.string(forKey: "username")

    var baseURL: String {
        serverURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    func saveLogin(token: String, username: String, serverURL: String) {
        self.token = token
        self.username = username
        self.serverURL = serverURL
        self.isLoggedIn = true
        KeychainHelper.save("jwt_token", token)
        UserDefaults.standard.set(username, forKey: "username")
        UserDefaults.standard.set(serverURL, forKey: "server_url")
    }

    func logout() {
        self.token = nil
        self.isLoggedIn = false
        KeychainHelper.delete("jwt_token")
    }
}

struct RootView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        if appState.isLoggedIn {
            MainTabView()
    