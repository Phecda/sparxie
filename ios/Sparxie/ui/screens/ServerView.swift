import SwiftUI

struct ServerView: View {
    @Environment(AppSettingsStore.self) private var settings
    @Environment(IperfSessionRuntime.self) private var runtime
    @FocusState private var isPortFocused: Bool
    @State private var actionError: String?

    var body: some View {
        @Bindable var settings = settings

        List {
            Section("Server") {
                TextField("Server Port", text: $settings.serverPort)
                #if os(iOS)
                    .keyboardType(.numberPad)
                #endif
                    .focused($isPortFocused)
                    .disabled(isServerActive)
            }

            Section("Status") {
                LabeledContent("Status") { Text(serverStatusText) }
                LabeledContent("Error") { Text(displayError ?? "-") }
            }

            Section("Live Data") {
                if runtime.jsonEvents.isEmpty {
                    Text("No JSON events yet.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(runtime.jsonEvents.reversed()) { event in
                        DisclosureGroup {
                            Text(event.json)
                                .font(.system(.caption, design: .monospaced))
                                .textSelection(.enabled)
                        } label: {
                            Text(event.name ?? String(localized: "event"))
                        }
                    }
                }
            }
        }
        .navigationTitle("Server")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                serverActionButton
            }
        }
        .onChange(of: isPortFocused) { _, _ in
            settings.persistServer()
        }
        .onDisappear {
            settings.persistServer()
        }
    }

    @ViewBuilder
    private var serverActionButton: some View {
        if isServerActive {
            Button {
                stopServer()
            } label: {
                Image(systemName: "stop.fill")
            }
            .accessibilityLabel("Stop Server")
            .help("Stop Server")
            .disabled(isStopping)
        } else {
            Button {
                startServer()
            } label: {
                Image(systemName: "play.fill")
            }
            .accessibilityLabel("Run Server")
            .help("Run Server")
            .buttonStyle(.borderedProminent)
        }
    }

    private var isServerActive: Bool {
        switch runtime.state {
        case .starting(.server), .running(.server), .stopping(.server):
            true
        default:
            false
        }
    }

    private var isStopping: Bool {
        if case .stopping(.server) = runtime.state {
            return true
        }

        return false
    }

    private var serverStatusText: LocalizedStringKey {
        switch runtime.state {
        case .starting(.server):
            "Starting"
        case .running(.server):
            "Listening"
        case .stopping(.server):
            "Stopping"
        case .stopped(.server):
            "Stopped"
        case .finished(.server):
            "Finished"
        case .failed(.server, _):
            "Failed"
        default:
            "Idle"
        }
    }

    private var displayError: String? {
        actionError ?? sessionError
    }

    private var sessionError: String? {
        if case .failed(.server, let failure) = runtime.state {
            return failure.message
        }

        return nil
    }

    private func startServer() {
        settings.persistServer()
        actionError = nil

        let portText = settings.serverPort.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let port = Int(portText), (1...65_535).contains(port) else {
            actionError = String(
                localized: "Server Port must be a whole number between 1 and 65535."
            )
            return
        }

        do {
            try runtime.startServer(IperfServerConfiguration(serverPort: port))
        } catch {
            actionError = error.localizedDescription
        }
    }

    private func stopServer() {
        actionError = nil
        runtime.stop()
    }
}

#Preview {
    NavigationStack {
        ServerView()
    }
    .environment(IperfSessionRuntime.shared)
    .environment(AppSettingsStore.preview())
}
