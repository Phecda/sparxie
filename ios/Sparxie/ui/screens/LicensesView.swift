import SwiftUI

struct LicensesView: View {
    var body: some View {
        EmptyStateView(
            systemImage: "doc.text",
            title: "Licenses",
            message: "License information will be available here."
        )
        .navigationTitle("Licenses")
    }
}

#Preview {
    NavigationStack {
        LicensesView()
    }
}
