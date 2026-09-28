import Foundation
import Iperf3
import Observation

enum IperfSessionKind: Sendable, Equatable {
    case client
    case server
}

struct IperfClientConfiguration: Sendable {
    let serverAddress: String
    let serverPort: Int
    let parallelStreams: Int

    init(
        serverAddress: String,
        serverPort: Int = 5201,
        parallelStreams: Int = 1
    ) {
        self.serverAddress = serverAddress
        self.serverPort = serverPort
        self.parallelStreams = parallelStreams
    }
}

struct IperfServerConfiguration: Sendable {
    let serverPort: Int

    init(serverPort: Int = 5201) {
        self.serverPort = serverPort
    }
}

struct IperfJSONEvent: Sendable, Equatable {
    let name: String?
    let json: String
}

struct IperfSessionFailure: Error, Equatable, Sendable, LocalizedError {
    let code: Int32
    let message: String

    var errorDescription: String? {
        message
    }
}

enum IperfSessionRuntimeError: Error, Equatable, Sendable, LocalizedError {
    case alreadyRunning
    case invalidServerAddress
    case invalidServerPort
    case invalidParallelStreams

    var errorDescription: String? {
        switch self {
        case .alreadyRunning:
            "An iperf session is already running."
        case .invalidServerAddress:
            "Server Address must not be empty."
        case .invalidServerPort:
            "Server Port must be between 1 and 65535."
        case .invalidParallelStreams:
            "Parallel Streams must be between 1 and 128."
        }
    }
}

enum IperfSessionState: Equatable, Sendable {
    case idle
    case starting(IperfSessionKind)
    case running(IperfSessionKind)
    case stopping(IperfSessionKind)
    case stopped(IperfSessionKind)
    case finished(IperfSessionKind)
    case failed(kind: IperfSessionKind, failure: IperfSessionFailure)
}

@MainActor
@Observable
final class IperfSessionRuntime {
    static let shared = IperfSessionRuntime()

    private(set) var state: IperfSessionState = .idle
    var onJSONEvent: ((IperfJSONEvent) -> Void)?

    private let executor: IperfSessionExecutor

    private init() {
        executor = IperfSessionExecutor()
    }

    func startClient(_ configuration: IperfClientConfiguration) throws {
        guard activeKind == nil else {
            throw IperfSessionRuntimeError.alreadyRunning
        }

        guard (1...65_535).contains(configuration.serverPort) else {
            throw IperfSessionRuntimeError.invalidServerPort
        }

        guard (1...128).contains(configuration.parallelStreams) else {
            throw IperfSessionRuntimeError.invalidParallelStreams
        }

        let serverAddress = configuration.serverAddress.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !serverAddress.isEmpty else {
            throw IperfSessionRuntimeError.invalidServerAddress
        }

        let normalizedConfiguration = IperfClientConfiguration(
            serverAddress: serverAddress,
            serverPort: configuration.serverPort,
            parallelStreams: configuration.parallelStreams
        )

        try start(.client(normalizedConfiguration))
    }

    func startServer(_ configuration: IperfServerConfiguration) throws {
        guard activeKind == nil else {
            throw IperfSessionRuntimeError.alreadyRunning
        }

        guard (1...65_535).contains(configuration.serverPort) else {
            throw IperfSessionRuntimeError.invalidServerPort
        }

        try start(.server(configuration))
    }

    func stop() {
        guard let kind = activeKind else {
            return
        }

        if executor.stop() {
            state = .stopping(kind)
        }
    }

    private func start(_ configuration: IperfSessionExecutor.Configuration) throws {
        try executor.start(
            configuration,
            jsonEventDelivery: { [weak self] json in
                Task { @MainActor [weak self] in
                    self?.deliverJSON(json)
                }
            },
            completion: { [weak self] event in
                Task { @MainActor [weak self] in
                    self?.handle(event)
                }
            }
        )

        state = .starting(configuration.kind)
    }

    private var activeKind: IperfSessionKind? {
        switch state {
        case .starting(let kind), .running(let kind), .stopping(let kind):
            kind
        case .idle, .stopped, .finished, .failed:
            nil
        }
    }

    private func handle(_ event: IperfSessionExecutor.Event) {
        switch event {
        case .running(let kind):
            guard case .starting = state else {
                return
            }
            state = .running(kind)
        case .stopped(let kind):
            guard activeKind == kind else {
                return
            }
            state = .stopped(kind)
        case .finished(let kind):
            guard activeKind == kind else {
                return
            }
            state = .finished(kind)
        case .failed(let kind, let failure):
            guard activeKind == kind else {
                return
            }
            state = .failed(kind: kind, failure: failure)
        }
    }

    private func deliverJSON(_ json: String) {
        guard let onJSONEvent else {
            return
        }

        onJSONEvent(IperfJSONEvent(name: Self.eventName(in: json), json: json))
    }

    private static func eventName(in json: String) -> String? {
        guard
            let data = json.data(using: .utf8),
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }

        return object["event"] as? String
    }
}

private nonisolated func iperfJSONCallback(
    _ test: OpaquePointer?,
    _ payload: UnsafeMutablePointer<CChar>?
) {
    IperfJSONCallbackRegistry.shared.dispatch(test: test, payload: payload)
}

private nonisolated final class IperfJSONCallbackRegistry: @unchecked Sendable {
    static let shared = IperfJSONCallbackRegistry()

    private let lock = NSLock()
    private var handlers: [OpaquePointer: @Sendable (String) -> Void] = [:]

    private init() {}

    func register(_ test: OpaquePointer, handler: @escaping @Sendable (String) -> Void) {
        lock.lock()
        handlers[test] = handler
        lock.unlock()
    }

    func unregister(_ test: OpaquePointer) {
        lock.lock()
        handlers.removeValue(forKey: test)
        lock.unlock()
    }

    func dispatch(test: OpaquePointer?, payload: UnsafeMutablePointer<CChar>?) {
        guard let test, let payload else {
            return
        }

        let json = String(cString: payload)

        lock.lock()
        let handler = handlers[test]
        lock.unlock()

        handler?(json)
    }
}

private nonisolated final class IperfSessionExecutor: @unchecked Sendable {
    enum Configuration: Sendable {
        case client(IperfClientConfiguration)
        case server(IperfServerConfiguration)

        var kind: IperfSessionKind {
            switch self {
            case .client:
                .client
            case .server:
                .server
            }
        }
    }

    enum Event: Sendable {
        case running(IperfSessionKind)
        case stopped(IperfSessionKind)
        case finished(IperfSessionKind)
        case failed(IperfSessionKind, IperfSessionFailure)
    }

    typealias Completion = @Sendable (Event) -> Void
    typealias JSONEventDelivery = @Sendable (String) -> Void

    private let lock = NSLock()
    private let workerQueue = DispatchQueue(
        label: "me.phecda.sparxie.iperf-session.worker",
        qos: .userInitiated
    )
    private let stopRetryQueue = DispatchQueue(
        label: "me.phecda.sparxie.iperf-session.stop",
        qos: .userInitiated
    )

    private var isActive = false
    private var stopRequested = false
    private var activeTest: OpaquePointer?

    func start(
        _ configuration: Configuration,
        jsonEventDelivery: @escaping JSONEventDelivery,
        completion: @escaping Completion
    ) throws {
        lock.lock()
        guard !isActive else {
            lock.unlock()
            throw IperfSessionRuntimeError.alreadyRunning
        }

        isActive = true
        stopRequested = false
        activeTest = nil
        lock.unlock()

        workerQueue.async { [weak self] in
            self?.run(
                configuration,
                jsonEventDelivery: jsonEventDelivery,
                completion: completion
            )
        }
    }

    @discardableResult
    func stop() -> Bool {
        lock.lock()
        guard isActive, !stopRequested else {
            lock.unlock()
            return false
        }

        stopRequested = true
        if let activeTest {
            iperf_interrupt(activeTest)
        }
        lock.unlock()

        stopRetryQueue.async { [weak self] in
            self?.retryInterruptUntilStopped()
        }

        return true
    }

    private func retryInterruptUntilStopped() {
        while true {
            lock.lock()
            guard isActive else {
                lock.unlock()
                return
            }

            if let activeTest {
                iperf_interrupt(activeTest)
            }
            lock.unlock()

            Thread.sleep(forTimeInterval: 0.02)
        }
    }

    private func run(
        _ configuration: Configuration,
        jsonEventDelivery: @escaping JSONEventDelivery,
        completion: @escaping Completion
    ) {
        let kind = configuration.kind

        guard let test = iperf_new_test() else {
            finishWithoutTest(event: .failed(kind, currentFailure()), completion: completion)
            return
        }

        publish(test)
        IperfJSONCallbackRegistry.shared.register(test, handler: jsonEventDelivery)

        guard iperf_defaults(test) >= 0 else {
            complete(test, event: .failed(kind, currentFailure()), completion: completion)
            return
        }

        guard set_protocol(test, Ptcp) >= 0 else {
            complete(test, event: .failed(kind, currentFailure()), completion: completion)
            return
        }

        configure(test, with: configuration)

        guard !isStopRequested() else {
            complete(test, event: .stopped(kind), completion: completion)
            return
        }

        completion(.running(kind))

        i_errno = Int32(IENONE)
        let result: Int32
        switch configuration {
        case .client:
            result = iperf_run_client(test)
        case .server:
            result = iperf_run_server(test)
        }

        let errorCode = i_errno
        let stopped = isStopRequested()
        let event: Event

        if stopped {
            event = .stopped(kind)
        } else if result < 0 {
            event = .failed(kind, failure(for: errorCode))
        } else {
            event = .finished(kind)
        }

        complete(test, event: event, completion: completion)
    }

    private func configure(_ test: OpaquePointer, with configuration: Configuration) {
        iperf_set_test_json_output(test, 1)
        iperf_set_test_json_stream(test, 1)
        iperf_set_test_json_callback(test, iperfJSONCallback)

        switch configuration {
        case .client(let client):
            iperf_set_test_role(test, 99)
            iperf_set_test_server_hostname(test, client.serverAddress)
            iperf_set_test_server_port(test, Int32(client.serverPort))
            iperf_set_test_duration(test, 10)
            iperf_set_test_num_streams(test, Int32(client.parallelStreams))
            iperf_set_test_reporter_interval(test, 1)
            iperf_set_test_stats_interval(test, 1)
            iperf_set_test_connect_timeout(test, 3_000)
            iperf_set_test_reverse(test, 0)
        case .server(let server):
            iperf_set_test_role(test, 115)
            iperf_set_test_server_port(test, Int32(server.serverPort))
            iperf_set_test_one_off(test, 0)
            iperf_set_test_reporter_interval(test, 1)
            iperf_set_test_stats_interval(test, 1)
        }
    }

    private func publish(_ test: OpaquePointer) {
        lock.lock()
        activeTest = test
        lock.unlock()
    }

    private func isStopRequested() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return stopRequested
    }

    private func complete(
        _ test: OpaquePointer,
        event: Event,
        completion: @escaping Completion
    ) {
        IperfJSONCallbackRegistry.shared.unregister(test)
        clearActiveTest(test)
        iperf_free_test(test)
        completion(event)
    }

    private func finishWithoutTest(event: Event, completion: @escaping Completion) {
        lock.lock()
        activeTest = nil
        isActive = false
        lock.unlock()
        completion(event)
    }

    private func clearActiveTest(_ test: OpaquePointer) {
        lock.lock()
        if activeTest == test {
            activeTest = nil
        }
        isActive = false
        lock.unlock()
    }

    private func currentFailure() -> IperfSessionFailure {
        failure(for: i_errno)
    }

    private func failure(for code: Int32) -> IperfSessionFailure {
        let message: String
        if let error = iperf_strerror(code) {
            message = String(cString: error)
        } else {
            message = "iperf error \(code)"
        }

        return IperfSessionFailure(code: code, message: message)
    }
}
