package me.phecda.sparxie.runtime

import me.phecda.iperf3.Iperf3Native
import me.phecda.iperf3.IperfJsonListener

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONException
import org.json.JSONObject

enum class IperfSessionKind {
    CLIENT,
    SERVER,
}

data class IperfClientConfiguration(
    val serverAddress: String,
    val serverPort: Int = 5201,
    val parallelStreams: Int = 1,
)

data class IperfServerConfiguration(
    val serverPort: Int = 5201,
)

data class IperfSessionFailure(
    val code: Int,
    val message: String,
)

data class IperfJsonEvent(
    val id: Long,
    val name: String?,
    val json: String,
)

sealed interface IperfSessionState {
    data object Idle : IperfSessionState

    data class Starting(val kind: IperfSessionKind) : IperfSessionState

    data class Running(val kind: IperfSessionKind) : IperfSessionState

    data class Stopping(val kind: IperfSessionKind) : IperfSessionState

    data class Stopped(val kind: IperfSessionKind) : IperfSessionState

    data class Finished(val kind: IperfSessionKind) : IperfSessionState

    data class Failed(
        val kind: IperfSessionKind,
        val failure: IperfSessionFailure,
    ) : IperfSessionState
}

sealed class IperfSessionRuntimeException(message: String) : IllegalStateException(message) {
    data object AlreadyRunning : IperfSessionRuntimeException("An iperf session is already running.")

    data object InvalidServerAddress : IperfSessionRuntimeException("Server Address must not be empty.")

    data object InvalidServerPort : IperfSessionRuntimeException("Server Port must be between 1 and 65535.")

    data object InvalidParallelStreams : IperfSessionRuntimeException("Parallel Streams must be between 1 and 128.")
}

object IperfSessionRuntime {
    private const val MAX_JSON_EVENT_COUNT = 100
    private const val MAX_PARALLEL_STREAMS = 128

    private val sessionLock = Any()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "iperf-session-worker")
    }
    private val nextJsonEventId = AtomicLong(0)

    private var activeKind: IperfSessionKind? = null
    private var stopRequested = false

    private val mutableState = MutableStateFlow<IperfSessionState>(IperfSessionState.Idle)
    val state: StateFlow<IperfSessionState> = mutableState.asStateFlow()

    private val mutableJsonEvents = MutableStateFlow<List<IperfJsonEvent>>(emptyList())
    val jsonEvents: StateFlow<List<IperfJsonEvent>> = mutableJsonEvents.asStateFlow()

    fun startClient(configuration: IperfClientConfiguration) {
        val serverAddress = configuration.serverAddress.trim()
        if (serverAddress.isEmpty()) {
            throw IperfSessionRuntimeException.InvalidServerAddress
        }
        validateServerPort(configuration.serverPort)
        if (configuration.parallelStreams !in 1..MAX_PARALLEL_STREAMS) {
            throw IperfSessionRuntimeException.InvalidParallelStreams
        }

        startSession(
            kind = IperfSessionKind.CLIENT,
            configuration = NativeSessionConfiguration.Client(
                serverAddress = serverAddress,
                serverPort = configuration.serverPort,
                parallelStreams = configuration.parallelStreams,
            ),
        )
    }

    fun startServer(configuration: IperfServerConfiguration) {
        validateServerPort(configuration.serverPort)

        startSession(
            kind = IperfSessionKind.SERVER,
            configuration = NativeSessionConfiguration.Server(
                serverPort = configuration.serverPort,
            ),
        )
    }

    fun stop() {
        synchronized(sessionLock) {
            val kind = activeKind
            if (kind != null && !stopRequested) {
                stopRequested = true
                mutableState.value = IperfSessionState.Stopping(kind)
                Iperf3Native.interrupt()
            }
        }
    }

    private fun startSession(
        kind: IperfSessionKind,
        configuration: NativeSessionConfiguration,
    ) {
        synchronized(sessionLock) {
            if (activeKind != null) {
                throw IperfSessionRuntimeException.AlreadyRunning
            }

            Iperf3Native.reset()
            activeKind = kind
            stopRequested = false
            mutableJsonEvents.value = emptyList()
            mutableState.value = IperfSessionState.Starting(kind)
        }

        executor.execute {
            runSession(kind, configuration)
        }
    }

    private fun runSession(
        kind: IperfSessionKind,
        configuration: NativeSessionConfiguration,
    ) {
        val handle = try {
            when (configuration) {
                is NativeSessionConfiguration.Client -> Iperf3Native.prepareClient(
                    serverAddress = configuration.serverAddress,
                    serverPort = configuration.serverPort,
                    parallelStreams = configuration.parallelStreams,
                )

                is NativeSessionConfiguration.Server -> Iperf3Native.prepareServer(
                    serverPort = configuration.serverPort,
                )
            }
        } catch (throwable: Throwable) {
            finishSession(
                kind = kind,
                terminalState = IperfSessionState.Failed(
                    kind = kind,
                    failure = failureFromThrowable(throwable),
                ),
            )
            return
        }

        if (handle == 0L) {
            finishSession(
                kind = kind,
                terminalState = IperfSessionState.Failed(
                    kind = kind,
                    failure = nativeFailure("Unable to prepare the iperf session."),
                ),
            )
            return
        }

        synchronized(sessionLock) {
            if (!stopRequested) {
                mutableState.value = IperfSessionState.Running(kind)
            }
        }

        val result = try {
            Iperf3Native.run(handle, jsonListener)
        } catch (throwable: Throwable) {
            finishSession(
                kind = kind,
                terminalState = IperfSessionState.Failed(
                    kind = kind,
                    failure = failureFromThrowable(throwable),
                ),
            )
            return
        }

        val stopped = synchronized(sessionLock) { stopRequested }
        when {
            stopped || result == Iperf3Native.RESULT_STOPPED -> {
                finishSession(
                    kind = kind,
                    terminalState = IperfSessionState.Stopped(kind),
                )
            }

            result == Iperf3Native.RESULT_FINISHED -> {
                finishSession(
                    kind = kind,
                    terminalState = IperfSessionState.Finished(kind),
                )
            }

            else -> {
                finishSession(
                    kind = kind,
                    terminalState = IperfSessionState.Failed(
                        kind = kind,
                        failure = nativeFailure("The iperf session failed."),
                    ),
                )
            }
        }
    }

    private fun finishSession(
        kind: IperfSessionKind,
        terminalState: IperfSessionState,
    ) {
        synchronized(sessionLock) {
            if (activeKind != kind) {
                return
            }

            activeKind = null
            stopRequested = false
            mutableState.value = terminalState
        }
    }

    private fun validateServerPort(serverPort: Int) {
        if (serverPort !in 1..65_535) {
            throw IperfSessionRuntimeException.InvalidServerPort
        }
    }

    private fun nativeFailure(fallbackMessage: String): IperfSessionFailure {
        val code = runCatching { Iperf3Native.lastErrorCode() }.getOrDefault(-1)
        val message = runCatching { Iperf3Native.lastErrorMessage(code) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: fallbackMessage

        return IperfSessionFailure(code = code, message = message)
    }

    private fun failureFromThrowable(throwable: Throwable): IperfSessionFailure {
        return IperfSessionFailure(
            code = -1,
            message = throwable.message?.takeIf { it.isNotBlank() }
                ?: throwable.javaClass.simpleName,
        )
    }

    private val jsonListener = IperfJsonListener(::handleJsonEvent)

    private fun handleJsonEvent(json: String) {
        val event = IperfJsonEvent(
            id = nextJsonEventId.getAndIncrement(),
            name = jsonEventName(json),
            json = json,
        )

        mutableJsonEvents.update { events ->
            if (events.size < MAX_JSON_EVENT_COUNT) {
                events + event
            } else {
                events.takeLast(MAX_JSON_EVENT_COUNT - 1) + event
            }
        }
    }

    private fun jsonEventName(json: String): String? {
        return try {
            JSONObject(json).opt("event") as? String
        } catch (_: JSONException) {
            null
        }
    }

    private sealed interface NativeSessionConfiguration {
        data class Client(
            val serverAddress: String,
            val serverPort: Int,
            val parallelStreams: Int,
        ) : NativeSessionConfiguration

        data class Server(
            val serverPort: Int,
        ) : NativeSessionConfiguration
    }
}