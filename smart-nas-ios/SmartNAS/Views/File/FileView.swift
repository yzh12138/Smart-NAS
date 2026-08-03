import SwiftUI
import UniformTypeIdentifiers

struct FileListView: View {
    @EnvironmentObject var appState: AppState
    @State private var files: [FileStorage] = []
    @State private var isLoading = true
    @State private var showImporter = false

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    LoadingView()
                } else if files.isEmpty {
                    EmptyStateView(icon: "folder", title: "暂无文件", subtitle: "点击右上角上传文件")
                } else {
                    List {
                        ForEach(files) { file in
                            HStack {
                                Image(systemName: "doc.fill")
                                    .font(.title2).foregroundColor(.appPrimary)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(file.originalName).font(.body).lineLimit(1)
                                    Text("\(formatFileSize(file.fileSize)) · \(file.uploadTime)")
                                        .font(.caption).foregroundColor(.secondary)
                                }
                                Spacer()
                            }
                            .padding(.vertical, 4)
                        }
                        .onDelete { indexSet in
                            for index in indexSet {
                                let file = files[index]
                                Task { try? await APIService.shared.deleteFile(token: appState.token!, id: file.id) }
                            }
                            files.remove(atOffsets: indexSet)
                        }
                    }
                }
            }
            .navigationTitle("文件存储")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { showImporter = true }) {
                        Image(systemName: "arrow.up.doc")
                    }
                }
            }
            .fileImporter(isPresented: $showImporter, allowedContentTypes: [.data], allowsMultiple: false) { result in
                if case .success(let url) = result {
                    Task { await uploadFile(url: url) }
                }
            }
            .task { await loadFiles() }
        }
    }

    private func loadFiles() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getFileList(token: token)
            await MainActor.run { self.files = result.records; self.isLoading = false }
        } catch { await MainActor.run { self.isLoading = false } }
    }

    private func uploadFile(url: URL) async {
        guard let token = appState.token else { return }
        guard url.startAccessingSecurityScopedResource() else { return }
        defer { url.stopAccessingSecurityScopedResource() }
        do {
            let data = try Data(contentsOf: url)
            _ = try await APIService.shared.uploadFile(token: token, fileData: data, fileName: url.last