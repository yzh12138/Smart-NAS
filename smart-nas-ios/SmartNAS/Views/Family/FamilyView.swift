import SwiftUI

struct FamilyView: View {
    @EnvironmentObject var appState: AppState
    @State private var families: [Family] = []
    @State private var isLoading = true
    @State private var showCreate = false
    @State private var showJoin = false
    @State private var familyName = ""
    @State private var inviteCode = ""
    @State private var selectedFamily: Family? = nil

    var body: some View {
        Group {
            if isLoading {
                LoadingView()
            } else if selectedFamily != nil {
                FamilyDetailView(family: selectedFamily!, onBack: { selectedFamily = nil })
            } else if families.isEmpty {
                EmptyStateView(icon: "house.fill", title: "暂无家庭", subtitle: "创建或加入一个家庭")
            } else {
                List {
                    ForEach(families) { family in
                        Button(action: { selectedFamily = family }) {
                            HStack {
                                Image(systemName: "house.fill")
                                    .font(.title2).foregroundColor(.appPrimary)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(family.name).font(.body)
                                    Text("\(family.memberCount) 位成员 · 邀请码: \(family.inviteCode)")
                                        .font(.caption).foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle(selectedFamily?.name ?? "家庭共享")
        .toolbar {
            if selectedFamily == nil {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack {
                        Button(action: { showJoin = true }) { Image(systemName: "person.badge.plus") }
                        Button(action: { showCreate = true }) { Image(systemName: "plus") }
                    }
                }
            }
        }
        .alert("创建家庭", isPresented: $showCreate) {
            TextField("家庭名称", text: $familyName)
            Button("创建") { createFamily() }
            Button("取消", role: .cancel) {}
        }
        .alert("加入家庭", isPresented: $showJoin) {
            TextField("邀请码", text: $inviteCode)
            Button("加入") { joinFamily() }
            Button("取消", role: .cancel) {}
        }
        .task { await loadFamilies() }
    }

    private func loadFamilies() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getMyFamilies(token: token)
            await MainActor.run { self.families = result; self.isLoading = false }
        } catch { await MainActor.run { self.isLoading = false } }
    }

    private func createFamily() {
        guard let token = appState.token else { return }
        Task {
            _ = try? await APIService.shared.createFamily(token: token, name: familyName)
            familyName = ""
            await loadFamilies()
        }
    }

    private func joinFamily() {
        guard let token = appState.token else { return }
        Task {
            do {
                let family = try await APIService.shared.searchFamilyByCode(token: token, code: inviteCode)
                try await APIService.shared.joinFamily(token: token, familyId: family.id)
                inviteCode = ""
                await loadFamilies()
            } catch {}
        }
    }
}

struct FamilyDetailView: View {
    @EnvironmentObject var appState: AppState
    let family: Family
    let onBack: () -> Void
    @State private var members: [FamilyMember] = []
    @State private var media: [Photo] = []
    @State private var tab = 0

    var body: some View {
        VStack {
            Picker("", selection: $tab) {
                Text("成员").tag(0)
                Text("共享照片").tag(1)
            }
            .pickerStyle(.segmented)
            .padding()

            if tab == 0 {
                List(members) { m in
                    HStack {
                        Image(systemName: "person.circle.fill").font(.title2).foregroundColor(.appPrimary)
                        VStack(alignment: .leading) {
                            Text(m.nickname.isEmpty ? m.username : m.nickname)
                            Text(m.status == 1 ? "已加入" : "待审核")
                                .font(.caption).foregroundColor(m.status == 1 ? .green : .orange)
                        }
                    }
                }
            } else {
                if media.isEmpty {
                    EmptyStateView(icon: "photo", title: "暂无共享照片")
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 100))], spacing: 4) {
                            ForEach(media) { photo in
                                NavigationLink { PhotoDetailView(photoId: photo.id) } label: {
                                    PhotoGridItem(photo: photo, baseURL: appState.baseURL) {}
                                        .allowsHitTesting(false)
                                }
                            }
                        }
                        .padding()
                    }
                }
            }
        }
        .task {
            guard let token = appState.token else { return }
            async let membersData = APIService.shared.getFamilyMembers(token: token, familyId: family.id)
            async let mediaData = APIService.shared.getFamilyMedia(token: token, familyId: family.id)
            if let m = try? await membersData { await 