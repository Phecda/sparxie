import SwiftUI

struct ServerView: View {
    @State private var runtime = IperfSessionRuntime.shared
    @State private var serverPort = "5201"
    @State private var actionError: String?

    var body: some View {
        List {
            Section("Server") {
                TextField("Server Port", text: $serverPort)
                #if os(iOS)
                    .keyboardType(.numberPad)
                #endif
                    .disabled(isServerActive)
            }

            Section("Status") {
                LabeledContent("Status", value: serverStatusText)
                LabeledContent("Error", value: displayError ?? "-")
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
                            Text(event.name ?? "event")
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

    private var serverStatusText: String {
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
        actionError = nil

        let portText = serverPort.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let port = Int(portText), (1...65_535).contains(port) else {
            actionError = "Server Port must be a whole number between 1 and 65535."
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
}
