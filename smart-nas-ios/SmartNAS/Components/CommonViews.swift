import SwiftUI

struct LoadingView: View {
    var body: some View {
        VStack(spacing: 12) {
            ProgressView()
            Text("加载中...").foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct EmptyStateView: View {
    let icon: String
    let title: String
    let subtitle: String

    init(icon: String, title: String, subtitle: String = "") {
        self.icon = icon
        self.title = title
        self.subtitle = subtitle
    }

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 60))
                .foregroundColor(.secondary.opacity(0.4))
            Text(title)
                .font(.title3)
                .foregroundColor(.secondary)
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundColor(.secondary.opacity(0.7))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct PhotoGridItem: View {
    let photo: Photo
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            AsyncImage(url: imageURL(id: photo.id)) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().aspectRatio(1, contentMode: .fill)
                case .failure:
                    Rectangle().fill(Color.gray.opacity(0.2)).overlay {
                        Image(systemName: "photo").foregroundColor(.gray)
                    }
                default:
                    Rectangle().fill(Color.gray.opacity(0.1)).overlay(ProgressView())
                }
            }
            .frame(minWidth: 0)
            .aspectRatio(1, contentMode: .fill)
            .clipShape(RoundedRectangle(cornerRadius: 6))
        }
        .buttonStyle(.plain)
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            Text(value)
                .font(.title.bold())
                .foregroundColor(.primary)
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .backgrou