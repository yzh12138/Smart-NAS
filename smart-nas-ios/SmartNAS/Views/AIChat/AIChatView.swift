import SwiftUI

struct AIChatListView: View {
    @EnvironmentObject var appState: AppState
    @State private var conversations: [Conversation] = []
    @State private var isLoading = true

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    LoadingView()
                } else if conversations.isEmpty {
                    EmptyStateView(icon: "bubble.left.and.bubble.right", title: "暂无对话", subtitle: "点击右上角 + 开始新对话")
                } else {
                    List {
                        ForEach(conversations) { conv in
                            NavigationLink { ConversationView(conversationId: conv.id) } label: {
                                HStack {
                                    Image(systemName: "bubble.left.fill")
                                        .font(.title2).foregroundColor(.appPrimary)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(conv.title).font(.body)
                                        Text(conv.updateTime).font(.caption).foregroundColor(.secondary)
                                    }
                                }
                            }
                        }
                        .onDelete { indexSet in
                            for index in indexSet {
                                Task { try? await APIService.shared.deleteConversation(token: appState.token!, id: conversations[index].id) }
                            }
                            conversations.remove(atOffsets: indexSet)
                        }
                    }
                }
            }
            .navigationTitle("AI 对话")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: createConversation) {
                        Image(systemName: "plus")
                    }
                }
            }
            .task { await loadConversations() }
        }
    }

    private func loadConversations() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getConversations(token: token)
            await MainActor.run { self.conversations = result; self.isLoading = false }
        } catch { await MainActor.run { self.isLoading = false } }
    }

    private func createConversation() {
        guard let token = appState.token else { return }
        Task {
            do {
                let conv = try await APIService.shared.createConversation(token: token)
                await MainActor.run { conversations.insert(conv, at: 0) }
            } catch {}
        }
    }
}

struct ConversationView: View {
    @EnvironmentObject var appState: AppState
    let conversationId: Int
    @State private var messages: [ChatMessage] = []
    @State private var inputText = ""
    @State private var isLoading = true
    @State private var isSending = false
    @FocusState private var isInputFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            // Messages
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(messages) { msg in
                            ChatBubbleView(message: msg)
                                .id(msg.id)
                        }
                        if isSending {
                            HStack {
                                ProgressView().scaleEffect(0.8)
                                Text("AI 正在思考...").font(.caption).foregroundColor(.secondary)
                                Spacer()
                            }
                            .padding(.horizontal)
                        }
                    }
                    .padding()
                }
                .onChange(of: messages.count) { _, _ in
                    if let last = messages.last {
                        withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                    }
                }
            }

            Divider()

            // Input
            HStack(spacing: 10) {
                TextField("输入消息...", text: $inputText, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(1...4)
                    .focused($isInputFocused)

                Button(action: sendMessage) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(inputText.isEmpty ? .gray : .appPrimary)
                }
                .disabled(inputText.isEmpty || isSending)
            }
            .padding()
        }
        .navigationTitle("AI 对话")
        .navigationBarTitleDisplayMode(.inline)
        .task { await loadMessages() }
    }

    private func loadMessages() async {
        guard let token = appState.token else { return }
        do {
            let result = try await APIService.shared.getConversationMessages(token: token, id: conversationId)
            await MainActor.run { self.messages = result; self.isLoading = false }
        } catch { await MainActor.run { self.isLoading = false } }
    }

    private func sendMessage() {
        guard let token = appState.token, !inputText.isEmpty else { return }
        let text = inputText
        inputText = ""
        isSending = true

        // Add user message immediately
        let userMsg = ChatMessage(role: "user", content: text, createTime: "")
        messages.append(userMsg)

        Task {
            do {
                let reply = try await APIService.shared.sendChatMessage(token: token, conversationId: conversationId, content: text)
                await MainActor.run {
                    messages.append(reply)
                    isSending = false
                }
            } catch { await MainActor.run { isSending = false } }
        }
    }
}

struct ChatBubbleView: View {
    let message: ChatMessage
    var isUser: Bool { message.role == "user" }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 60) }

            if !isUser {
                Image(systemName: "brain.head.profile")
                    .font(.title3)
                    .foregroundColor(.white)
                    .frame(width: 32, height: 32)
                    .background(Color.appPrimary)
                    .clipShape(Circle())
            }

            Text(message.content)
                .padding(12)
                .background(isUser ? Color.appPrimary : Color(.systemGray5))
                .foregroundColor(isUser ? .white : .primary)
