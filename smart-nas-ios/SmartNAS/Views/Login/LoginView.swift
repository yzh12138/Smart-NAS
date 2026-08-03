import SwiftUI

struct LoginView: View {
    @EnvironmentObject var appState: AppState
    @State private var serverURL: String = ""
    @State private var username = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showServerSettings = false
    @State private var isSecured = true

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 32) {
                    Spacer(minLength: 60)

                    // Logo
                    VStack(spacing: 12) {
                        Image(systemName: "cloud.fill")
                            .font(.system(size: 64))
                            .foregroundColor(.appPrimary)
                        Text("Smart NAS")
                            .font(.largeTitle.bold())
                            .foregroundColor(.appPrimary)
                        Text("智能照片管理系统")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    // Form
                    VStack(spacing: 16) {
                        // Server URL
                        DisclosureGroup("服务器设置", isExpanded: $showServerSettings) {
                            TextField("http://192.168.1.100:8080", text: $serverURL)
                                .textFieldStyle(.roundedBorder)
                                .autocapitalization(.none)
                                .disableAutocorrection(true)
                                .textContentType(.URL)
                        }
                        .padding(.horizontal, 4)

                        // Username
                        HStack {
                            Image(systemName: "person.fill").foregroundColor(.secondary)
                            TextField("用户名", text: $username)
                                .textContentType(.username)
                                .autocapitalization(.none)
                        }
                        .padding(12)
                        .background(Color(.systemGray6))
                        .clipShape(RoundedRectangle(cornerRadius: 10))

                        // Password
                        HStack {
                            Image(systemName: "lock.fill").foregroundColor(.secondary)
                            if isSecured {
                                SecureField("密码", text: $password)
                            } else {
                                TextField("密码", text: $password)
                            }
                            Button(action: { isSecured.toggle() }) {
                                Image(systemName: isSecured ? "eye.slash.fill" : "eye.fill")
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(12)
                        .background(Color(.systemGray6))
                        .clipShape(RoundedRectangle(cornerRadius: 10))

                        // Error
                        if !errorMessage.isEmpty {
                            Text(errorMessage)
                                .font(.caption)
                                .foregroundColor(.red)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        // Login Button
                        Button(action: login) {
                            if isLoading {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                            } else {
                                Text("登 录")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                            }
                        }
                        .background(Color.appPrimary)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .disabled(username.isEmpty || password.isEmpty || isLoading)
                    }
                    .padding(.horizontal, 24)

                    Spacer()
                }
            }
            .background(Color.appBackground.ignoresSafeArea())
            .onAppear {
                serverURL = appState.serverURL
            }
        }
    }

    private func login() {
        isLoading = true
        errorMessage = ""

        Task {
            do {
                let response = try await APIService.shared.login(
                    serverURL: serverURL,
                    username: username,
                    password: password
                )
                await MainActor.run {
                    appState.saveLogin(
                        token: response.token,
                        username: username,
                        serverURL: serverURL
                    )
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    