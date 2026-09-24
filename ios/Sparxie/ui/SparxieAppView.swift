import SwiftUI

struct SparxieAppView: View {
    var body: some View {
        TabView {
            NavigationStack {
                ClientView()
            }
            .tabItem {
                Label("Client", systemImage: "arrow.up.right.circle")
            }

            NavigationStack {
                ServerView()
            }
            .tabItem {
                Label("Server", systemImage: "server.rack")
            }

            NavigationStack {
                MoreView()
            }
            .tabItem {
                Label("More", systemImage: "ellipsis.circle")
            }
        }
    }
}

#Preview {
    SparxieAppView()
}
