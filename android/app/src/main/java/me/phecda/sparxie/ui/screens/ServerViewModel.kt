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
import me.phecda.sparxie.runtime.IperfServerConfiguration
import me.phecda.sparxie.runtime.IperfSessionRuntime
import me.phecda.sparxie.runtime.IperfSessionState

data class ServerUiState(
    val portInput: String = "5201",
    val sessionState: IperfSessionState = IperfSessionState.Idle,
    val jsonEvents: List<IperfJsonEvent> = emptyList(),
    val expandedJsonEventIds: Set<Long> = emptySet(),
    val actionError: String? = null,
)

class ServerViewModel : ViewModel() {
    private val runtime = IperfSessionRuntime
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<ServerUiState> = combine(
        runtime.state,
        runtime.jsonEvents,
        localState,
    ) { sessionState, jsonEvents, localState ->
        ServerUiState(
            portInput = localState.portInput,
            sessionState = sessionState,
            jsonEvents = jsonEvents.asReversed().toList(),
            expandedJsonEventIds = localState.expandedJsonEventIds,
            actionError = localState.actionError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ServerUiState(),
    )

    fun onServerPortChange(value: String) {
        localState.update { it.copy(portInput = value) }
    }

    fun onStart() {
        val port = localState.value.portInput.trim().toIntOrNull()
        if (port == null || port !in 1..65_535) {
            localState.update {
                it.copy(actionError = "Server Port must be between 1 and 65535.")
            }
            return
        }

        localState.update { it.copy(actionError = null) }
        runCatching {
            runtime.startServer(IperfServerConfiguration(serverPort = port))
        }.onFailure { throwable ->
            localState.update {
                it.copy(actionError = throwable.message ?: "Unable to start the server.")
            }
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
        val portInput: String = "5201",
        val expandedJsonEventIds: Set<Long> = emptySet(),
        val actionError: String? = null,
    )
}
