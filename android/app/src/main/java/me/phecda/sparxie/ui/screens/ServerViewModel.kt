package me.phecda.sparxie.ui.screens

import androidx.annotation.StringRes
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import me.phecda.sparxie.R
import me.phecda.sparxie.SparxieApplication
import me.phecda.sparxie.runtime.IperfJsonEvent
import me.phecda.sparxie.runtime.IperfServerConfiguration
import me.phecda.sparxie.runtime.IperfSessionRuntime
import me.phecda.sparxie.runtime.IperfSessionRuntimeException
import me.phecda.sparxie.runtime.IperfSessionState

data class ServerUiState(
    val portInput: String = "5201",
    val sessionState: IperfSessionState = IperfSessionState.Idle,
    val jsonEvents: List<IperfJsonEvent> = emptyList(),
    val expandedJsonEventIds: Set<Long> = emptySet(),
    @param:StringRes val actionErrorRes: Int? = null,
)

class ServerViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = (application as SparxieApplication).serverSettingsStore
    private val runtime = IperfSessionRuntime
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<ServerUiState> = combine(
        runtime.state,
        runtime.jsonEvents,
        localState,
        settingsStore.state,
    ) { sessionState, jsonEvents, localState, settings ->
        ServerUiState(
            portInput = settings.portInput,
            sessionState = sessionState,
            jsonEvents = jsonEvents.asReversed().toList(),
            expandedJsonEventIds = localState.expandedJsonEventIds,
            actionErrorRes = localState.actionErrorRes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ServerUiState(portInput = settingsStore.state.value.portInput),
    )

    fun onServerPortChange(value: String) {
        settingsStore.setPort(value)
    }

    fun onStart() {
        val port = settingsStore.state.value.portInput.trim().toIntOrNull()
        if (port == null || port !in 1..65_535) {
            localState.update {
                it.copy(actionErrorRes = R.string.error_server_port_range)
            }
            return
        }

        localState.update { it.copy(actionErrorRes = null) }
        runCatching {
            runtime.startServer(IperfServerConfiguration(serverPort = port))
        }.onFailure { throwable ->
            localState.update {
                it.copy(actionErrorRes = serverActionError(throwable))
            }
        }
    }

    fun onLocalNetworkPermissionDenied() {
        localState.update {
            it.copy(
                actionErrorRes = R.string.error_local_network_permission_required,
            )
        }
    }

    fun onStop() {
        localState.update { it.copy(actionErrorRes = null) }
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
        @param:StringRes val actionErrorRes: Int? = null,
    )

    @StringRes
    private fun serverActionError(throwable: Throwable): Int {
        return when (throwable) {
            IperfSessionRuntimeException.AlreadyRunning -> R.string.error_iperf_session_already_running
            IperfSessionRuntimeException.InvalidServerPort -> R.string.error_server_port_range
            else -> R.string.error_unable_start_server
        }
    }
}
