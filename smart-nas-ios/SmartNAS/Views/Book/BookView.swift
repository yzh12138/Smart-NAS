import SwiftUI
import UniformTypeIdentifiers

struct BookListView: View {
    @EnvironmentObject var appState: AppState
    @State private var books: [Book] = []
    @State private var isLoading = true
    @State private var showImporter = false

    var body: some View {
        Group {
            if isLoading {
                LoadingView()
            } else if books.isEmpty {
                EmptyStateView(icon: "book.closed", title: "暂无图书", subtitle: "上传 EPUB 或 PDF 文件")
            } else {
                List {
                    ForEach(books) { book in
                        HStack {
                            RoundedRectangle(cornerRadius: 6)
                                .fill(Color.appTertiary.opacity(0.2))
                                .frame(width: 44, height: 56)
                                .overlay(Image(systemName: "book.closed").foregroundColor(.appTertiary))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(book.title).font(.body).lineLimit(1)
                                if !book.author.isEmpty {
                                    Text(book.author).font(.caption).foregroundColor(.secondary)
                                }
                                Text("\(book.fileType.uppercased()) · \(formatFileSize(book.fileSize))")
                                    .font(.caption2).foregroundColor(.secondary)
                            }
                        }
                    }
                    .onDelete { indexSet in
                        for index in indexSet {
                            Task { try? await APIService.shared.deleteBook(token: appState.token!, id: books[index].id) }
                        }
                        books.remove(atOffsets: indexSet)
                    }
                }
            }
        }
        .navigationTitle("图书管理")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: { showImporter = true }) { Image(systemName: "plus") }
            }
        }
        .fileImporter(isPresented: $showImporter, allowedContentTypes: [.epub, .pdf], allowsMultiple: false) { _ in }
        .task { await loadBooks() }
    }

    private func loadBooks() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getBookList(token: token)
            await MainActor.run { self.books = result.records; self.isLoading = false }
        } catch { 