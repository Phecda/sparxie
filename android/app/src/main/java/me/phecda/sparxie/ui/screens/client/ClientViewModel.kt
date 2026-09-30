package me.phecda.sparxie.ui.screens.client

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
import me.phecda.sparxie.runtime.IperfClientConfiguration
import me.phecda.sparxie.runtime.IperfSessionRuntime
import me.phecda.sparxie.runtime.IperfSessionRuntimeException
import me.phecda.sparxie.runtime.IperfSessionState

data class ClientUiState(
    val addressInput: String = "",
    val portInput: String = "5201",
    val parallelStreamsInput: String = "1",
    val sessionState: IperfSessionState = IperfSessionState.Idle,
    val jsonEvents: List<IperfJsonEvent> = emptyList(),
    val expandedJsonEventIds: Set<Long> = emptySet(),
    @param:StringRes val actionErrorRes: Int? = null,
)

class ClientViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = (application as SparxieApplication).clientSettingsStore
    private val runtime = IperfSessionRuntime
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<ClientUiState> = combine(
        runtime.state,
        runtime.jsonEvents,
        localState,
        settingsStore.state,
    ) { sessionState, jsonEvents, localState, settings ->
        ClientUiState(
            addressInput = settings.addressInput,
            portInput = settings.portInput,
            parallelStreamsInput = settings.parallelStreamsInput,
            sessionState = sessionState,
            jsonEvents = jsonEvents.asReversed().toList(),
            expandedJsonEventIds = localState.expandedJsonEventIds,
            actionErrorRes = localState.actionErrorRes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClientUiState(
            addressInput = settingsStore.state.value.addressInput,
            portInput = settingsStore.state.value.portInput,
            parallelStreamsInput = settingsStore.state.value.parallelStreamsInput,
        ),
    )

    fun onServerAddressChange(value: String) {
        settingsStore.setAddress(value)
    }

    fun onServerPortChange(value: String) {
        settingsStore.setPort(value)
    }

    fun onParallelStreamsChange(value: String) {
        settingsStore.setParallelStreams(value)
    }

    fun onStart() {
        val settings = settingsStore.state.value
        val address = settings.addressInput.trim()
        if (address.isEmpty()) {
            localState.update {
                it.copy(actionErrorRes = R.string.error_server_address_empty)
            }
            return
        }

        val port = settings.portInput.trim().toIntOrNull()
        if (port == null || port !in 1..65_535) {
            localState.update {
                it.copy(actionErrorRes = R.string.error_server_port_range)
            }
            return
        }

        val parallelStreams = settings.parallelStreamsInput.trim().toIntOrNull()
        if (parallelStreams == null || parallelStreams !in 1..128) {
            localState.update {
                it.copy(actionErrorRes = R.string.error_parallel_streams_range)
            }
            return
        }

        localState.update { it.copy(actionErrorRes = null) }
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
                it.copy(actionErrorRes = clientActionError(throwable))
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
        val addressInput: String = "",
        val portInput: String = "5201",
        val parallelStreamsInput: String = "1",
        val expandedJsonEventIds: Set<Long> = emptySet(),
        @param:StringRes val actionErrorRes: Int? = null,
    )

    @StringRes
    private fun clientActionError(throwable: Throwable): Int {
        return when (throwable) {
            IperfSessionRuntimeException.AlreadyRunning -> R.string.error_iperf_session_already_running
            IperfSessionRuntimeException.InvalidServerAddress -> R.string.error_server_address_empty
            IperfSessionRuntimeException.InvalidServerPort -> R.string.error_server_port_range
            IperfSessionRuntimeException.InvalidParallelStreams -> R.string.error_parallel_streams_range
            else -> R.string.error_unable_start_client
        }
    }
}
