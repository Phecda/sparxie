import SwiftUI

struct ServerView: View {
    var body: some View {
        List {
            Section {
                NavigationLink(value: ServerDestination.bindAddress) {
                    Label {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Bind Address")
                            Text("Choose the local address for the server.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    } icon: {
                        Image(systemName: "network")
                    }
                }
            }

            Section {
                EmptyStateView(
                    systemImage: "server.rack",
                    title: "Server",
                    message: "Server setup will be available in a later stage."
                )
                .frame(maxWidth: .infinity, minHeight: 240)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Server")
        .navigationDestination(for: ServerDestination.self) { destination in
            switch destination {
            case .bindAddress:
                ServerBindAddressView()
            }
        }
    }
}

#Preview {
    NavigationStack {
        ServerView()
    }
}
