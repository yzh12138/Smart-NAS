import SwiftUI
import PhotosUI

struct PhotoGalleryView: View {
    @EnvironmentObject var appState: AppState
    @State private var photos: [Photo] = []
    @State private var tags: [Tag] = []
    @State private var isLoading = true
    @State private var selectedTag: String? = nil
    @State private var searchText = ""

    let columns = [GridItem(.adaptive(minimum: 110), spacing: 3)]

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    LoadingView()
                } else if photos.isEmpty {
                    EmptyStateView(icon: "photo", title: "暂无照片", subtitle: "点击右上角 + 上传照片")
                } else {
                    ScrollView {
                        // Tag filters
                        if !tags.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    TagChip(name: "全部", isSelected: selectedTag == nil) { selectedTag = nil }
                                    ForEach(tags.prefix(6)) { tag in
                                        TagChip(name: tag.name, isSelected: selectedTag == tag.name) {
                                            selectedTag = selectedTag == tag.name ? nil : tag.name
                                        }
                                    }
                                }
                                .padding(.horizontal)
                            }
                            .padding(.vertical, 8)
                        }

                        LazyVGrid(columns: columns, spacing: 3) {
                            ForEach(photos) { photo in
                                NavigationLink { PhotoDetailView(photoId: photo.id) } label: {
                                    PhotoGridItem(photo: photo) {}
                                        .allowsHitTesting(false)
                                }
                            }
                        }
                        .padding(3)
                    }
                }
            }
            .navigationTitle("照片总览")
            .searchable(text: $searchText, prompt: "搜索照片...")
            .onChange(of: searchText) { _, newValue in
                Task {
                    if newValue.isEmpty {
                        await loadPhotos()
                    } else {
                        await searchPhotos(newValue)
                    }
                }
            }
            .onChange(of: selectedTag) { _, _ in
                Task { await loadPhotos() }
            }
            .task {
                await loadPhotos()
                await loadTags()
            }
        }
    }

    private func loadPhotos() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getPhotoList(token: token, size: 100, tag: selectedTag)
            await MainActor.run {
                self.photos = result.records
                self.isLoading = false
            }
        } catch { await MainActor.run { self.isLoading = false } }
    }

    private func loadTags() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getTagList(token: token)
            await MainActor.run { self.tags = result }
        } catch {}
    }

    private func searchPhotos(_ keyword: String) async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.searchPhotos(token: token, keyword: keyword)
            await MainActor.run { self.photos = result }
        } catch {}
    }
}

struct TagChip: View {
    let name: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(name)
                .font(.subheadline)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(isSelected ? Color.appPrimary : Color(.systemGray5))
          