import SwiftUI

struct VideoView: View {
    @EnvironmentObject var appState: AppState
    @State private var videos: [Photo] = []
    @State private var isLoading = true

    let columns = [GridItem(.adaptive(minimum: 160), spacing: 10)]

    var body: some View {
        Group {
            if isLoading {
                LoadingView()
            } else if videos.isEmpty {
                EmptyStateView(icon: "video", title: "暂无视频", subtitle: "上传视频后会显示在这里")
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 10) {
                        ForEach(videos) { video in
                            NavigationLink { PhotoDetailView(photoId: video.id) } label: {
                                VStack(alignment: .leading) {
                                    ZStack {
                                        AsyncImage(url: imageURL(id: video.id)) { phase in
                                            if let image = phase.image {
                                                image.resizable().aspectRatio(16/9, contentMode: .fill)
                                            } else {
                                                Rectangle().fill(Color.gray.opacity(0.2)).aspectRatio(16/9, contentMode: .fill)
                                            }
                                        }
                                        Image(systemName: "play.circle.fill")
                                            .font(.system(size: 40))
                                            .foregroundColor(.white.opacity(0.8))
                                    }
                                    .clipShape(RoundedRectangle(cornerRadius: 10))

                                    Text(video.name)
                                        .font(.subheadline).lineLimit(1)
                                    if let city = video.city {
                                        Text(city).font(.caption).foregroundColor(.secondary)
                                    }
                                }
                            }
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("视频管理")
        .task { await loadVideos() }
    }

    private func loadVideos() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getPhotoList(token: token, size: 100, mediaType: 1)
            await MainActor.run { self.videos = result.records; self.isLoading = false }
        } catch { 