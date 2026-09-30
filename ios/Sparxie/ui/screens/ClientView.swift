import SwiftUI

struct ClientView: View {
    @Environment(IperfSessionRuntime.self) private var runtime
    @State private var serverAddress = ""
    @State private var serverPort = "5201"
    @State private var parallelStreams = "1"
    @State private var actionError: String?

    var body: some View {
        List {
            Section("Client") {
                TextField("Server Address", text: $serverAddress)
                    .disabled(isClientActive)

                TextField("Server Port", text: $serverPort)
                #if os(iOS)
                    .keyboardType(.numberPad)
                #endif
                    .disabled(isClientActive)

                TextField("Parallel Streams", text: $parallelStreams)
                #if os(iOS)
                    .keyboardType(.numberPad)
                #endif
                    .disabled(isClientActive)
            }

            Section("Status") {
                LabeledContent("Status") { Text(clientStatusText) }
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
        .navigationTitle("Client")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                clientActionButton
            }
        }
    }

    @ViewBuilder
    private var clientActionButton: some View {
        if isClientActive {
            Button {
                stopClient()
            } label: {
                Image(systemName: "stop.fill")
            }
            .accessibilityLabel("Stop Client")
            .help("Stop Client")
            .disabled(isStopping)
        } else {
            Button {
                startClient()
            } label: {
                Image(systemName: "play.fill")
            }
            .accessibilityLabel("Run Client")
            .help("Run Client")
            .buttonStyle(.borderedProminent)
        }
    }

    private var isClientActive: Bool {
        switch runtime.state {
        case .starting(.client), .running(.client), .stopping(.client):
            true
        default:
            false
        }
    }

    private var isStopping: Bool {
        if case .stopping(.client) = runtime.state {
            return true
        }

        return false
    }

    private var clientStatusText: LocalizedStringKey {
        switch runtime.state {
        case .starting(.client):
            "Starting"
        case .running(.client):
            "Running"
        case .stopping(.client):
            "Stopping"
        case .stopped(.client):
            "Stopped"
        case .finished(.client):
            "Finished"
        case .failed(.client, _):
            "Failed"
        default:
            "Idle"
        }
    }

    private var displayError: String? {
        actionError ?? sessionError
    }

    private var sessionError: String? {
        if case .failed(.client, let failure) = runtime.state {
            return failure.message
        }

        return nil
    }

    private func startClient() {
        actionError = nil

        let address = serverAddress.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !address.isEmpty else {
            actionError = IperfSessionRuntimeError.invalidServerAddress.localizedDescription
            return
        }

        let portText = serverPort.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let port = Int(portText), (1...65_535).contains(port) else {
            actionError = String(
                localized: "Server Port must be a whole number between 1 and 65535."
            )
            return
        }

        let streamsText = parallelStreams.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let streams = Int(streamsText), (1...128).contains(streams) else {
            actionError = String(
                localized: "Parallel Streams must be a whole number between 1 and 128."
            )
            return
        }

        do {
            try runtime.startClient(
                IperfClientConfiguration(
                    serverAddress: address,
                    serverPort: port,
                    parallelStreams: streams
                )
            )
        } catch {
            actionError = error.localizedDescription
        }
    }

    private func stopClient() {
        actionError = nil
        runtime.stop()
    }
}

#Preview {
    NavigationStack {
        ClientView()
    }
    .environment(IperfSessionRuntime.shared)
}
