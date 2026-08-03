import SwiftUI

struct TagsView: View {
    @EnvironmentObject var appState: AppState
    @State private var tags: [Tag] = []
    @State private var showCreate = false
    @State private var newName = ""

    var body: some View {
        Group {
            if tags.isEmpty {
                EmptyStateView(icon: "tag", title: "暂无标签")
            } else {
                List {
                    ForEach(tags) { tag in
                        HStack {
                            Circle().fill(Color(hex: tag.color)).frame(width: 14, height: 14)
                            Text(tag.name).font(.body)
                            Spacer()
                        }
                    }
                    .onDelete { indexSet in
                        for index in indexSet {
                            Task { try? await APIService.shared.deleteTag(token: appState.token!, id: tags[index].id) }
                        }
                        tags.remove(atOffsets: indexSet)
                    }
                }
            }
        }
        .navigationTitle("标签管理")
        .toolbar {
            Button(action: { showCreate = true }) { Image(systemName: "plus") }
        }
        .alert("新建标签", isPresented: $showCreate) {
            TextField("标签名称", text: $newName)
            Button("创建") { createTag() }
            Button("取消", role: .cancel) {}
        }
        .task { await loadTags() }
    }

    private func loadTags() async {
        guard let token = appState.token else { return }
        if let result = try? await APIService.shared.getTagList(token: token) {
            await MainActor.run { self.tags = result }
        }
    }

    private func createTag() {
        guard let token = appState.token, !newName.isEmpty else { return }
        Task {
            _ = try? await APIService.shared.createTag(token: token, name: newName)
            newName = ""
            await loadTags()
        }
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r, g, b: Double
        switch hex.count {
        case 6:
            r = Double((int >> 16) & 0xFF) / 255
            g = Double((int >> 8) & 0xFF) / 255
            b = Double(int & 0xFF) / 255
        default:
            r = 0.25; g = 0.46; b