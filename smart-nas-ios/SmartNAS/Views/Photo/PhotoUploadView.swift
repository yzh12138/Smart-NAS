import SwiftUI
import PhotosUI

struct PhotoUploadView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedItems: [PhotosPickerItem] = []
    @State private var selectedImages: [UIImage] = []
    @State private var tags = ""
    @State private var city = ""
    @State private var isUploading = false
    @State private var uploadResult = ""
    @State private var showSuccess = false

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Photo Picker
                PhotosPicker(selection: $selectedItems, maxSelectionCount: 20, matching: .images) {
                    if selectedImages.isEmpty {
                        VStack(spacing: 12) {
                            Image(systemName: "photo.badge.plus")
                                .font(.system(size: 48))
                                .foregroundColor(.appPrimary)
                            Text("点击选择照片")
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 200)
                        .background(Color(.systemGray6))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    } else {
                        ScrollView(.horizontal) {
                            HStack(spacing: 8) {
                                ForEach(selectedImages.indices, id: \.self) { index in
                                    Image(uiImage: selectedImages[index])
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(width: 150, height: 150)
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                            }
                        }
                    }
                }
                .onChange(of: selectedItems) { _, newItems in
                    Task {
                        selectedImages = []
                        for item in newItems {
                            if let data = try? await item.loadTransferable(type: Data.self),
                               let image = UIImage(data: data) {
                                selectedImages.append(image)
                            }
                        }
                    }
                }

                if !selectedImages.isEmpty {
                    Text("已选择 \(selectedImages.count) 张照片")
                        .font(.subheadline).foregroundColor(.secondary)
                }

                // Tags
                HStack {
                    Image(systemName: "tag.fill").foregroundColor(.secondary)
                    TextField("标签（逗号分隔）", text: $tags)
                }
                .padding(12)
                .background(Color(.systemGray6))
                .clipShape(RoundedRectangle(cornerRadius: 10))

                // City
                HStack {
                    Image(systemName: "location.fill").foregroundColor(.secondary)
                    TextField("城市", text: $city)
                }
                .padding(12)
                .background(Color(.systemGray6))
                .clipShape(RoundedRectangle(cornerRadius: 10))

                // Upload Button
                Button(action: upload) {
                    if isUploading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                    } else {
                        Label("上传照片", systemImage: "arrow.up.circle.fill")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                    }
                }
                .background(Color.appPrimary)
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .disabled(selectedImages.isEmpty || isUploading)

                // Result
                if showSuccess {
                    Label(uploadResult, systemImage: "checkmark.circle.fill")
                        .foregroundColor(.green)
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.green.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
            .padding()
        }
        .navigationTitle("上传照片")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func upload() {
        guard let token = appState.token else { return }
        isUploading = true

        Task {
            let fileData: [(Data, String)] = selectedImages.compactMap { image in
                guard let data = image.jpegData(compressionQuality: 0.8) else { return nil }
                return (data, "\(UUID().uuidString).jpg")
            }

            do {
                let result = try await APIService.shared.uploadPhotos(
                    token: token,
                    files: fileData,
                    tags: tags.isEmpty ? nil : tags,
                    city: city.isEmpty ? nil : city
                )
                await MainActor.run {
                    isUploading = false
                    showSuccess = true
                    uploadResult = "上传成功，共 \(result.count) 张照片"
                    selectedImages = []
                    selectedItems = []
                }
            } catch {
                await MainActor.run {
                    isUploading =