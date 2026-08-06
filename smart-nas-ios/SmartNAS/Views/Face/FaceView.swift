import SwiftUI

struct FaceView: View {
    @EnvironmentObject var appState: AppState
    @State private var clusters: [FaceCluster] = []
    @State private var selectedCluster: FaceCluster? = nil
    @State private var photos: [Photo] = []
    @State private var isLoading = true
    @State private var renameText = ""
    @State private var renameTarget: FaceCluster? = nil

    let columns = [GridItem(.adaptive(minimum: 100), spacing: 4)]

    var body: some View {
        Group {
            if isLoading {
                LoadingView()
            } else if selectedCluster != nil {
                if photos.isEmpty {
                    EmptyStateView(icon: "photo", title: "暂无照片")
                } else {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 4) {
                            ForEach(photos) { photo in
                                NavigationLink { PhotoDetailView(photoId: photo.id) } label: {
                                    PhotoGridItem(photo: photo, baseURL: appState.baseURL) {}
                                        .allowsHitTesting(false)
                                }
                            }
                        }
                        .padding()
                    }
                }
            } else if clusters.isEmpty {
                EmptyStateView(icon: "face.smiling", title: "暂无人脸聚类", subtitle: "上传照片后系统会自动识别")
            } else {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 150))], spacing: 10) {
                        ForEach(clusters) { cluster in
                            VStack {
                                Image(systemName: "face.smiling.fill")
                                    .font(.system(size: 48))
                                    .foregroundColor(.appPrimary)
                                Text(cluster.name.isEmpty ? "未命名" : cluster.name)
                                    .font(.subheadline.bold())
                                Text("\(cluster.photoCount) 张照片")
                                    .font(.caption).foregroundColor(.secondary)
                                HStack(spacing: 12) {
                                    Button(action: { renameTarget = cluster; renameText = cluster.name }) {
                                        Image(systemName: "pencil").font(.caption)
                                    }
                                    Button(action: { deleteCluster(cluster.id) }) {
                                        Image(systemName: "trash").font(.caption).foregroundColor(.red)
                                    }
                                }
                            }
                            .padding()
                            .cardStyle()
                            .onTapGesture { selectCluster(cluster) }
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle(selectedCluster?.name ?? "人脸识别")
        .toolbar {
            if selectedCluster != nil {
                ToolbarItem(placement: .topBarLeading) {
                    Button("返回") { selectedCluster = nil; Task { await loadClusters() } }
                }
            }
        }
        .alert("重命名", isPresented: .constant(renameTarget != nil)) {
            TextField("名称", text: $renameText)
            Button("确定") { if let t = renameTarget { renameCluster(t.id, renameText); renameTarget = nil } }
            Button("取消", role: .cancel) { renameTarget = nil }
        }
        .task { await loadClusters() }
    }

    private func loadClusters() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getFaceClusters(token: token)
            await MainActor.run { self.clusters = result; self.isLoading = false }
        } catch { await MainActor.run { self.isLoading = false } }
    }

    private func selectCluster(_ cluster: FaceCluster) {
        selectedCluster = cluster
        Task {
            guard let token = appState.token else { return }
            if let result = try? await APIService.shared.getFaceClusterPhotos(token: token, clusterId: cluster.id) {
                await MainActor.run { self.photos = result }
            }
        }
    }

    private func renameCluster(_ id: Int, _ name: String) {
        Task {
            try? await APIService.shared.renameFaceCluster(token: appState.token!, id: id, name: name)
            await loadClusters()
        }
    }

    private func deleteCluster(_ id: Int) {
        Task {
            try? await APIService.