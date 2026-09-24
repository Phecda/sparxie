import SwiftUI

struct ClientView: View {
    var body: some View {
        EmptyStateView(
            systemImage: "arrow.up.right.circle",
            title: "Client",
            message: "Client testing will be available here."
        )
        .navigationTitle("Client")
    }
}

#Preview {
    NavigationStack {
        ClientView()
    }
}
