package me.phecda.sparxie.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import me.phecda.sparxie.runtime.IperfJsonEvent
import me.phecda.sparxie.runtime.IperfClientConfiguration
import me.phecda.sparxie.runtime.IperfSessionRuntime
import me.phecda.sparxie.runtime.IperfSessionState

data class ClientUiState(
    val addressInput: String = "",
    val portInput: String = "5201",
    val parallelStreamsInput: String = "1",
    val sessionState: IperfSessionState = IperfSessionState.Idle,
    val jsonEvents: List<IperfJsonEvent> = emptyList(),
    val expandedJsonEventIds: Set<Long> = emptySet(),
    val actionError: String? = null,
)

class ClientViewModel : ViewModel() {
    private val runtime = IperfSessionRuntime
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<ClientUiState> = combine(
        runtime.state,
        runtime.jsonEvents,
        localState,
    ) { sessionState, jsonEvents, localState ->
        ClientUiState(
            addressInput = localState.addressInput,
            portInput = localState.portInput,
            parallelStreamsInput = localState.parallelStreamsInput,
            sessionState = sessionState,
            jsonEvents = jsonEvents.asReversed().toList(),
            expandedJsonEventIds = localState.expandedJsonEventIds,
            actionError = localState.actionError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClientUiState(),
    )

    fun onServerAddressChange(value: String) {
        localState.update { it.copy(addressInput = value) }
    }

    fun onServerPortChange(value: String) {
        localState.update { it.copy(portInput = value) }
    }

    fun onParallelStreamsChange(value: String) {
        localState.update { it.copy(parallelStreamsInput = value) }
    }

    fun onStart() {
        val address = localState.value.addressInput.trim()
        if (address.isEmpty()) {
            localState.update {
                it.copy(actionError = "Server Address must not be empty.")
            }
            return
        }

        val port = localState.value.portInput.trim().toIntOrNull()
        if (port == null || port !in 1..65_535) {
            localState.update {
                it.copy(actionError = "Server Port must be between 1 and 65535.")
            }
            return
        }

        val parallelStreams = localState.value.parallelStreamsInput.trim().toIntOrNull()
        if (parallelStreams == null || parallelStreams !in 1..128) {
            localState.update {
                it.copy(actionError = "Parallel Streams must be between 1 and 128.")
            }
            return
        }

        localState.update { it.copy(actionError = null) }
        runCatching {
            runtime.startClient(
                IperfClientConfiguration(
                    serverAddress = address,
                    serverPort = port,
                    parallelStreams = parallelStreams,
                ),
            )
        }.onFailure { throwable ->
            localState.update {
                it.copy(actionError = throwable.message ?: "Unable to start the client.")
            }
        }
    }

    fun onLocalNetworkPermissionDenied() {
        localState.update {
            it.copy(
                actionError = "Local network permission is required. Allow Nearby devices in app settings.",
            )
        }
    }

    fun onStop() {
        localState.update { it.copy(actionError = null) }
        runtime.stop()
    }

    fun onToggleJsonEvent(id: Long) {
        localState.update { localState ->
            val expandedIds = localState.expandedJsonEventIds
            localState.copy(
                expandedJsonEventIds = if (id in expandedIds) {
                    expandedIds - id
                } else {
                    expandedIds + id
                },
            )
        }
    }

    private data class LocalState(
        val addressInput: String = "",
        val portInput: String = "5201",
        val parallelStreamsInput: String = "1",
        val expandedJsonEventIds: Set<Long> = emptySet(),
        val actionError: String? = null,
    )
}
