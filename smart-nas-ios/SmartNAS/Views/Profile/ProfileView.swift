import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var appState: AppState
    @State private var user: UserInfo? = nil
    @State private var isLoading = true

    var body: some View {
        Group {
            if isLoading {
                LoadingView()
            } else if let user = user {
                List {
                    Section {
                        VStack(spacing: 12) {
                            Circle()
                                .fill(Color.appPrimary)
                                .frame(width: 80, height: 80)
                                .overlay(
                                    Text(String((user.nickname.isEmpty ? user.username : user.nickname).prefix(1)))
                                        .font(.title).foregroundColor(.white)
                                )
                            Text(user.nickname.isEmpty ? user.username : user.nickname)
                                .font(.title2.bold())
                            Text("@\(user.username)")
                                .font(.subheadline).foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                    }

                    Section("基本信息") {
                        HStack { Text("用户名"); Spacer(); Text(user.username).foregroundColor(.secondary) }
                        HStack { Text("昵称"); Spacer(); Text(user.nickname.isEmpty ? "未设置" : user.nickname).foregroundColor(.secondary) }
                        HStack { Text("角色"); Spacer(); Text(user.roles.joined(separator: ", ").isEmpty ? "普通用户" : user.roles.joined(separator: ", ")).foregroundColor(.secondary) }
                    }
                }
            }
        }
        .navigationTitle("个人资料")
        .task { await loadProfile() }
    }

    private func loadProfile() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getUserInfo(token: token)
            await MainActor.run { self.user = result; self.isLoading = false }
        } catch { await Mai