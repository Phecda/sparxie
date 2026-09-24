import SwiftUI

struct MoreView: View {
    var body: some View {
        List {
            Section {
                NavigationLink(value: MoreDestination.settings) {
                    Label("Settings", systemImage: "gearshape")
                }

                NavigationLink(value: MoreDestination.licenses) {
                    Label("Licenses", systemImage: "doc.text")
                }
            }
        }
        .navigationTitle("More")
        .navigationDestination(for: MoreDestination.self) { destination in
            switch destination {
            case .settings:
                SettingsView()
            case .licenses:
                LicensesView()
            }
        }
    }
}

#Preview {
    NavigationStack {
        MoreView()
    }
}
