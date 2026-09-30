import MMKV
import Observation

@MainActor
@Observable
final class AppSettingsStore {
    static func preview() -> AppSettingsStore {
        MMKV.initialize(rootDir: nil)
        return AppSettingsStore()
    }

    var clientServerAddress: String
    var clientServerPort: String
    var clientParallelStreams: String
    var serverPort: String

    @ObservationIgnored
    private let kv: MMKV

    init(kv: MMKV? = nil) {
        if let kv {
            self.kv = kv
        } else {
            guard let kv = MMKV(mmapID: "sparxie.settings") else {
                fatalError("Unable to create MMKV settings store.")
            }
            self.kv = kv
        }

        clientServerAddress = self.kv.string(forKey: Keys.clientAddress) ?? ""
        clientServerPort = self.kv.string(forKey: Keys.clientPort) ?? "5201"
        clientParallelStreams = self.kv.string(forKey: Keys.clientParallelStreams) ?? "1"
        serverPort = self.kv.string(forKey: Keys.serverPort) ?? "5201"
    }

    func persistClient() {
        persist(clientServerAddress, forKey: Keys.clientAddress)
        persist(clientServerPort, forKey: Keys.clientPort)
        persist(clientParallelStreams, forKey: Keys.clientParallelStreams)
    }

    func persistServer() {
        persist(serverPort, forKey: Keys.serverPort)
    }

    private func persist(_ value: String, forKey key: String) {
        guard kv.set(value, forKey: key) else {
            fatalError("Unable to save setting: \(key)")
        }
    }

    private enum Keys {
        static let clientAddress = "client.address"
        static let clientPort = "client.port"
        static let clientParallelStreams = "client.parallel_streams"
        static let serverPort = "server.port"
    }
}
