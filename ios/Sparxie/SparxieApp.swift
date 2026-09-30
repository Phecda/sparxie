import MMKV
import SwiftUI

@main
struct SparxieApp: App {
    @State private var runtime = IperfSessionRuntime.shared
    @State private var settings: AppSettingsStore

    init() {
        MMKV.initialize(rootDir: nil)
        _settings = State(initialValue: AppSettingsStore())
    }

    var body: some Scene {
        WindowGroup {
            SparxieAppView()
                .environment(runtime)
                .environment(settings)
        }
    }
}
