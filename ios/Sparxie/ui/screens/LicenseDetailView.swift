import SwiftUI

struct LicenseDetailView: View {
    let license: LicenseEntry

    private var licenseText: String {
        guard let url = Bundle.main.url(
            forResource: license.resourceName,
            withExtension: "txt"
        ) else {
            return "License text is unavailable."
        }

        return (try? String(contentsOf: url, encoding: .utf8))
            ?? "License text is unavailable."
    }

    var body: some View {
        ScrollView {
            Text(licenseText)
                .frame(maxWidth: .infinity, alignment: .leading)
                .textSelection(.enabled)
                .font(.system(.footnote, design: .monospaced))
                .padding(24)
        }
    }
}

#Preview {
    NavigationStack {
        LicenseDetailView(license: licenseEntries[0])
            .navigationTitle(licenseEntries[0].name)
    }
}
