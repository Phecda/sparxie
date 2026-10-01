import Foundation

struct LicenseEntry: Identifiable, Hashable {
    let id: String
    let name: String
    let licenseName: String
    let resourceName: String
}

let licenseEntries = [
    LicenseEntry(
        id: "mmkv",
        name: "MMKV",
        licenseName: "BSD 3-Clause",
        resourceName: "mmkv_license"
    )
]
