import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @State private var showLogout = false

    var body: some View {
        NavigationStack {
            List {
                Section("账号") {
                    NavigationLink { ProfileView() } label: {
                        Label("个人资料", systemImage: "person.fill")
                    }
                }

                Section("内容管理") {
                    NavigationLink { TagsView() } label: { Label("标签管理", systemImage: "tag.fill") }
                    NavigationLink { FaceView() } label: { Label("人脸识别", systemImage: "face.smiling") }
                    NavigationLink { RecycleView() } label: { Label("回收站", systemImage: "trash") }
                }

                Section("社交") {
                    NavigationLink { FamilyView() } label: { Label("家庭共享", systemImage: "house.fill") }
                    NavigationLink { FriendView() } label: { Label("好友管理", systemImage: "person.2.fill") }
                }

                Section("媒体") {
                    NavigationLink { VideoView() } label: { Label("视频管理", systemImage: "video.fill") }
                    NavigationLink { FileListView() } label: { Label("文件存储", systemImage: "folder.fill") }
                    NavigationLink { BookListView() } label: { Label("图书管理", systemImage: "book.closed.fill") }
                }

                Section("关于") {
                    HStack {
                        Text("版本")
                        Spacer()
                        Text("v1.0.0").foregroundColor(.secondary)
                    }
                }

                Section {
                    Button(action: { showLogout = true }) {
                        HStack {
                            Spacer()
                            Text("退出登录").foregroundColor(.red)
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("设置")
            .alert("退出登录", isPresented: $showLogout) {
                Button("取消", role: .cancel) {}
                Button("确定", role: .destructive) { appState.logout() }
            } message: {
                Text("确定�