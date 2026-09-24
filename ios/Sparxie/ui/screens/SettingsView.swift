import SwiftUI

struct SettingsView: View {
    var body: some View {
        EmptyStateView(
            systemImage: "gearshape",
            title: "Settings",
            message: "Settings will be available here."
        )
        .navigationTitle("Settings")
    }
}

#Preview {
    NavigationStack {
        SettingsView()
    }
}
