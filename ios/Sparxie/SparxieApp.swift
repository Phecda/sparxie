import SwiftUI

@main struct SparxieApp: App {
    @State private var runtime = IperfSessionRuntime.shared

    var body: some Scene {
        WindowGroup {
            SparxieAppView()
                .environment(runtime)
        }
    }
}
