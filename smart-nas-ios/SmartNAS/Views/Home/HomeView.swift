import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem { Label("首页", systemImage: "house.fill") }
            PhotoGalleryView()
                .tabItem { Label("照片", systemImage: "photo.on.rectangle") }
            AIChatListView()
                .tabItem { Label("AI", systemImage: "brain.head.profile") }
            FileListView()
                .tabItem { Label("文件", systemImage: "folder.fill") }
            SettingsView()
                .tabItem { Label("设置", systemImage: "gearshape.fill") }
        }
    }
}

struct HomeView: View {
    @EnvironmentObject var appState: AppState
    @State private var photos: [Photo] = []
    @State private var cityStats: [CityStat] = []
    @State private var photoCount = 0
    @State private var videoCount = 0
    @State private var isLoading = true

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    LoadingView()
                } else {
                    ScrollView {
                        VStack(spacing: 20) {
                            // Stats
                            HStack(spacing: 12) {
                                StatCard(title: "照片", value: "\(photoCount)", icon: "photo.fill", color: .appPrimary)
                                StatCard(title: "视频", value: "\(videoCount)", icon: "video.fill", color: .appSecondary)
                            }
                            .padding(.horizontal)

                            // Quick Actions
                            HStack(spacing: 8) {
                                NavigationLink { PhotoUploadView() } label: {
                                    Label("上传照片", systemImage: "arrow.up.circle.fill")
                                        .font(.subheadline)
                                        .padding(.horizontal, 12).padding(.vertical, 8)
                                        .background(Color.appPrimary.opacity(0.1))
                                        .clipShape(Capsule())
                                }
                                NavigationLink { AIChatListView() } label: {
                                    Label("AI 对话", systemImage: "brain.head.profile")
                                        .font(.subheadline)
                                        .padding(.horizontal, 12).padding(.vertical, 8)
                                        .background(Color.appSecondary.opacity(0.1))
                                        .clipShape(Capsule())
                                }
                                Spacer()
                            }
                            .padding(.horizontal)

                            // City Stats
                            if !cityStats.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("城市分布").font(.headline)
                                    ForEach(cityStats.prefix(5)) { city in
                                        HStack {
                                            Text(city.city)
                                            Spacer()
                                            Text("\(city.count) 张").foregroundColor(.appPrimary)
                                        }
                                        .font(.subheadline)
                                    }
                                }
                                .padding()
                                .cardStyle()
                                .padding(.horizontal)
                            }

                            // Recent Photos
                            if !photos.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    HStack {
                                        Text("最近照片").font(.headline)
                                        Spacer()
                                        NavigationLink("查看全部") { PhotoGalleryView() }
                                            .font(.subheadline)
                                    }
                                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 100))], spacing: 4) {
                                        ForEach(photos.prefix(9)) { photo in
                                            NavigationLink { PhotoDetailView(photoId: photo.id) } label: {
                                                PhotoGridItem(photo: photo) {}
                                                    .allowsHitTesting(false)
                                            }
                                        }
                                    }
                                }
                                .padding()
                                .cardStyle()
                                .padding(.horizontal)
                            }
                        }
                        .padding(.vertical)
                    }
                }
            }
            .navigationTitle("Smart NAS")
            .background(Color.appBackground)
            .task { await loadDashboard() }
        }
    }

    private func loadDashboard() async {
        guard let token = appState.token else { return }
        do {
            async let photosResult = APIService.shared.getPhotoList(token: token, size: 9)
            async let videoResult = APIService.shared.getPhotoList(token: token, size: 1, mediaType: 1)
            async let cityResult = APIService.shared.getCityPhotoStats(token: token)

            let (photosData, videoData, cityData) = try await (photosResult, videoResult, cityResult)
            await MainActor.run {
                self.photos = photosData.records
                self.photoCount = photosData.total
                self.videoCount = videoData.total
                self.cityStats = cityData
                self.i