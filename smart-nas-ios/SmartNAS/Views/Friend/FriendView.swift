import SwiftUI

struct FriendView: View {
    @EnvironmentObject var appState: AppState
    @State private var friends: [Friend] = []
    @State private var requests: [FriendRequest] = []
    @State private var searchResults: [UserInfo] = []
    @State private var tab = 0
    @State private var searchText = ""

    var body: some View {
        VStack {
            Picker("", selection: $tab) {
                Text("好友列表").tag(0)
                Text("请求 (\(requests.count))").tag(1)
                Text("搜索").tag(2)
            }
            .pickerStyle(.segmented)
            .padding()

            switch tab {
            case 0:
                if friends.isEmpty {
                    EmptyStateView(icon: "person.2", title: "暂无好友")
                } else {
                    List(friends) { f in
                        HStack {
                            Image(systemName: "person.circle.fill").font(.title2).foregroundColor(.appPrimary)
                            Text(f.friendNickname.isEmpty ? f.friendName : f.friendNickname)
                            Spacer()
                            Button(action: { removeFriend(f.friendId) }) {
                                Image(systemName: "person.fill.xmark").foregroundColor(.red)
                            }
                        }
                    }
                }
            case 1:
                if requests.isEmpty {
                    EmptyStateView(icon: "tray", title: "暂无请求")
                } else {
                    List(requests) { r in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(r.fromNickname.isEmpty ? r.fromUsername : r.fromNickname)
                                Text(r.createTime).font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Button(action: { acceptRequest(r.id) }) {
                                Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
                            }
                            Button(action: { rejectRequest(r.id) }) {
                                Image(systemName: "xmark.circle.fill").foregroundColor(.red)
                            }
                        }
                    }
                }
            default:
                VStack {
                    HStack {
                        TextField("搜索用户", text: $searchText)
                            .textFieldStyle(.roundedBorder)
                        Button(action: { Task { await searchUsers() } }) {
                            Image(systemName: "magnifyingglass")
                        }
                    }
                    .padding(.horizontal)

                    List(searchResults) { user in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(user.nickname.isEmpty ? user.username : user.nickname)
                                Text("@\(user.username)").font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Button(action: { sendRequest(user.id) }) {
                                Image(systemName: "person.badge.plus").foregroundColor(.appPrimary)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("好友管理")
        .task { await loadData() }
    }

    private func loadData() async {
        guard let token = appState.token else { return }
        async let f = APIService.shared.getFriendList(token: token)
        async let r = APIService.shared.getPendingFriendRequests(token: token)
        if let friends = try? await f { await MainActor.run { self.friends = friends } }
        if let requests = try? await r { await MainActor.run { self.requests = requests } }
    }

    private func searchUsers() async {
        guard let token = appState.token, !searchText.isEmpty else { return }
        if let result = try? await APIService.shared.searchUsers(token: token, keyword: searchText) {
            await MainActor.run { self.searchResults = result }
        }
    }

    private func sendRequest(_ id: Int) {
        Task { try? await APIService.shared.sendFriendRequest(token: appState.token!, friendId: id) }
    }

    private func acceptRequest(_ id: Int) {
        Task {
            try? await APIService.shared.acceptFriendRequest(token: appState.token!, id: id)
            await loadData()
        }
    }

    private func rejectRequest(_ id: Int) {
        Task {
            try? await APIService.shared.rejectFriendRequest(token: appState.token!, id: id)
            await loadData()
        }
    }

    private func removeFriend(_ id: Int) {
        Task {
            try? a