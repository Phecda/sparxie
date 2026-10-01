import SwiftUI

struct LicensesView: View {
    var body: some View {
        List(licenseEntries) { license in
            NavigationLink {
                LicenseDetailView(license: license)
                    .navigationTitle(license.name)
            } label: {
                LabeledContent(license.name, value: license.licenseName)
            }
        }
        .navigationTitle("Licenses")
    }
}

#Preview {
    NavigationStack {
        LicensesView()
    }
}
