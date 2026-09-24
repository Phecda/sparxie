import SwiftUI

struct ServerBindAddressView: View {
    var body: some View {
        EmptyStateView(
            systemImage: "network",
            title: "Bind Address",
            message: "Bind address selection will be available in a later stage."
        )
        .navigationTitle("Bind Address")
    }
}

#Preview {
    NavigationStack {
        ServerBindAddressView()
    }
}
