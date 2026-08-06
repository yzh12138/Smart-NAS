import SwiftUI

struct PhotoDetailView: View {
    @EnvironmentObject var appState: AppState
    let photoId: Int
    @State private var photo: Photo? = nil
    @State private var comments: [PhotoComment] = []
    @State private var isLoading = true
    @State private var showComments = false
    @State private var newComment = ""
    @State private var showDeleteAlert = false

    var body: some View {
        Group {
            if isLoading {
                LoadingView()
            } else if let photo = photo {
                ScrollView {
                    VStack(spacing: 16) {
                        // Full Image
                        AsyncImage(url: imageURL(baseURL: appState.baseURL, id: photo.id, thumb: false)) { phase in
                            if let image = phase.image {
                                image.resizable().aspectRatio(contentMode: .fit)
                            } else {
                                Rectangle().fill(Color.gray.opacity(0.2)).aspectRatio(1, contentMode: .fit)
                                    .overlay(ProgressView())
                            }
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal)

                        // Info
                        VStack(alignment: .leading, spacing: 8) {
                            Text(photo.name).font(.title2.bold())

                            if let city = photo.city, !city.isEmpty {
                                Label(city, systemImage: "location.fill")
                                    .font(.subheadline).foregroundColor(.appPrimary)
                            }
                            if let shootTime = photo.shootTime {
                                Label(shootTime, systemImage: "calendar")
                                    .font(.subheadline).foregroundColor(.secondary)
                            }
                            Label("\(photo.clickCount) 次查看", systemImage: "eye.fill")
                                .font(.subheadline).foregroundColor(.secondary)
                        }
                        .padding()
                        .cardStyle()
                        .padding(.horizontal)

                        // Tags
                        if !photo.tags.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("标签").font(.headline)
                                FlowLayout(spacing: 8) {
                                    ForEach(photo.tags) { tag in
                                        Text(tag.name)
                                            .font(.caption)
                                            .padding(.horizontal, 10).padding(.vertical, 5)
                                            .background(Color.appPrimary.opacity(0.1))
                                            .clipShape(Capsule())
                                    }
                                }
                            }
                            .padding()
                            .cardStyle()
                            .padding(.horizontal)
                        }

                        // AI Tags
                        if let aiTags = photo.aiTags, !aiTags.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("AI 标签").font(.headline)
                                Text(aiTags).font(.body)
                            }
                            .padding()
                            .background(Color.appSecondary.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .padding(.horizontal)
                        }

                        // Comments
                        VStack(alignment: .leading, spacing: 12) {
                            Button(action: { showComments.toggle() }) {
                                HStack {
                                    Text("留言 (\(comments.count))").font(.headline)
                                    Spacer()
                                    Image(systemName: showComments ? "chevron.up" : "chevron.down")
                                }
                            }

                            if showComments {
                                ForEach(comments) { comment in
                                    HStack(alignment: .top, spacing: 10) {
                                        Circle()
                                            .fill(Color.appPrimary)
                                            .frame(width: 32, height: 32)
                                            .overlay(Text(String(comment.nickname.prefix(1))).foregroundColor(.white).font(.caption))
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(comment.nickname).font(.subheadline.bold())
                                            Text(comment.content).font(.body)
                                            Text(comment.createTime).font(.caption2).foregroundColor(.secondary)
                                        }
                                    }
                                }

                                HStack {
                                    TextField("添加留言...", text: $newComment)
                                        .textFieldStyle(.roundedBorder)
                                    Button(action: addComment) {
                                        Image(systemName: "paperplane.fill").foregroundColor(.appPrimary)
                                    }
                                    .disabled(newComment.isEmpty)
                                }
                            }
                        }
                        .padding()
                        .cardStyle()
                        .padding(.horizontal)
                    }
                    .padding(.vertical)
                }
            }
        }
        .navigationTitle("照片详情")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: { showDeleteAlert = true }) {
                    Image(systemName: "trash").foregroundColor(.red)
                }
            }
        }
        .alert("删除照片", isPresented: $showDeleteAlert) {
            Button("取消", role: .cancel) {}
            Button("删除", role: .destructive) { deletePhoto() }
        } message: {
            Text("确定要删除这张照片吗？")
        }
        .task { await loadDetail() }
    }

    private func loadDetail() async {
        guard let token = appState.token else { return }
        do {
            async let photoData = APIService.shared.getPhotoDetail(token: token, id: photoId)
            async let commentData = APIService.shared.getPhotoComments(token: token, photoId: photoId)
            try await APIService.shared.trackPhotoClick(token: token, id: photoId)
            let (p, c) = try await (photoData, commentData)
            await MainActor.run {
                self.photo = p
                self.comments = c
                self.isLoading = false
            }
        } catch { await MainActor.run { self.isLoading = false } }
    }

    private func addComment() {
        guard let token = appState.token else { return }
        Task {
            do {
                let comment = try await APIService.shared.addPhotoComment(token: token, photoId: photoId, content: newComment)
                await MainActor.run {
                    comments.append(comment)
                    newComment = ""
                }
            } catch {}
        }
    }

    private func deletePhoto() {
        guard let token = appState.token else { return }
        Task {
            try? await APIService.shared.deletePhoto(token: token, id: photoId)
        }
    }
}

// Simple flow layout
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0; y += rowHeight + spacing; rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: maxWidth, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x: CGFloat = bounds.minX, y: CGFloat = bounds.minY, rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX; y += rowHeight + spacing; rowHeight = 0
     