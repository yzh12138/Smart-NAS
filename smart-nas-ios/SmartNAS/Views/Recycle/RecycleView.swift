import SwiftUI

struct RecycleView: View {
    @EnvironmentObject var appState: AppState
    @State private var photos: [Photo] = []
    @State private var isLoading = true
    @State private var showEmptyAlert = false
    @State private var selectedPhoto: Photo? = nil

    let columns = [GridItem(.adaptive(minimum: 100), spacing: 4)]

    var body: some View {
        Group {
            if isLoading {
                LoadingView()
            } else if photos.isEmpty {
                EmptyStateView(icon: "trash", title: "回收站为空")
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 4) {
                        ForEach(photos) { photo in
                            PhotoGridItem(photo: photo) { selectedPhoto = photo }
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("回收站")
        .toolbar {
            if !photos.isEmpty {
                Button(action: { showEmptyAlert = true }) {
                    Image(systemName: "trash.fill").foregroundColor(.red)
                }
            }
        }
        .confirmationDialog("照片操作", isPresented: .constant(selectedPhoto != nil), titleVisibility: .visible) {
            if let photo = selectedPhoto {
                Button("恢复") { restorePhoto(photo.id); selectedPhoto = nil }
                Button("永久删除", role: .destructive) { permanentDelete(photo.id); selectedPhoto = nil }
                Button("取消", role: .cancel) { selectedPhoto = nil }
            }
        } message: {
            if let photo = selectedPhoto { Text(photo.name) }
        }
        .alert("清空回收站", isPresented: $showEmptyAlert) {
            Button("清空", role: .destructive) { emptyRecycle() }
            Button("取消", role: .cancel) {}
        } message: {
            Text("确定永久删除所有照片？此操作不可撤销。")
        }
        .task { await loadRecycle() }
    }

    private func loadRecycle() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getRecycleList(token: token)
            await MainActor.run { self.photos = result.records; self.isLoading = false }
        } catch { await MainActor.run { self.isLoading = false } }
    }

    private func restorePhoto(_ id: Int) {
        Task {
            try? await APIService.shared.restorePhoto(token: appState.token!, id: id)
            await loadRecycle()
        }
    }

    private func permanentDelete(_ id: Int) {
        Task {
            try? await APIService.shared.permanentDelete(token: appState.token!, id: id)
            await loadRecycle()
        }
    }

    private func emptyRecycle() {
        Task {
            try? await APIService.shared.emptyRecy