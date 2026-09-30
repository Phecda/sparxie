package me.phecda.sparxie.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.phecda.sparxie.runtime.IperfSessionKind
import me.phecda.sparxie.runtime.IperfSessionState

@Composable
fun ClientRoute(viewModel: ClientViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientScreen(
        state = uiState,
        onServerAddressChange = viewModel::onServerAddressChange,
        onServerPortChange = viewModel::onServerPortChange,
        onParallelStreamsChange = viewModel::onParallelStreamsChange,
        onToggleJsonEvent = viewModel::onToggleJsonEvent,
    )
}

@Composable
fun ClientToolbarAction(viewModel: ClientViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState = uiState.sessionState
    val isClientActive = isClientActive(sessionState)
    val isStopping = sessionState is IperfSessionState.Stopping &&
        sessionState.kind == IperfSessionKind.CLIENT
    val onStart = rememberLocalNetworkStart(
        onStart = viewModel::onStart,
        onPermissionDenied = viewModel::onLocalNetworkPermissionDenied,
    )

    FilledIconButton(
        onClick = if (isClientActive) viewModel::onStop else onStart,
        enabled = !isStopping,
    ) {
        Icon(
            imageVector = if (isClientActive) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isClientActive) "Stop Client" else "Start Client",
        )
    }
}

@Composable
fun ClientScreen(
    state: ClientUiState,
    onServerAddressChange: (String) -> Unit,
    onServerPortChange: (String) -> Unit,
    onParallelStreamsChange: (String) -> Unit,
    onToggleJsonEvent: (Long) -> Unit,
) {
    val sessionState = state.sessionState
    val isClientActive = isClientActive(sessionState)
    val errorMessage = state.actionError ?: failureMessage(sessionState) ?: "-"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.addressInput,
                onValueChange = onServerAddressChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClientActive,
                label = { Text("Server Address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        }

        item {
            OutlinedTextField(
                value = state.portInput,
                onValueChange = onServerPortChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClientActive,
                label = { Text("Server Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        item {
            OutlinedTextField(
                value = state.parallelStreamsInput,
                onValueChange = onParallelStreamsChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClientActive,
                label = { Text("Parallel Streams") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Status")
                Text(statusText(sessionState))
                Text("Error")
                Text(errorMessage)
            }
        }

        item {
            Text("Live Data")
        }

        if (state.jsonEvents.isEmpty()) {
            item {
                Text("No JSON events yet.")
            }
        } else {
            items(
                items = state.jsonEvents,
                key = { event -> event.id },
            ) { event ->
                Column {
                    ListItem(
                        modifier = Modifier.clickable {
                            onToggleJsonEvent(event.id)
                        },
                        headlineContent = { Text(event.name ?: "event") },
                        supportingContent = {
                            if (event.id in state.expandedJsonEventIds) {
                                SelectionContainer {
                                    Text(
                                        text = event.json,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun isClientActive(state: IperfSessionState): Boolean {
    return when (state) {
        is IperfSessionState.Starting -> state.kind == IperfSessionKind.CLIENT
        is IperfSessionState.Running -> state.kind == IperfSessionKind.CLIENT
        is IperfSessionState.Stopping -> state.kind == IperfSessionKind.CLIENT
        else -> false
    }
}

private fun statusText(state: IperfSessionState): String {
    return when {
        state is IperfSessionState.Starting && state.kind == IperfSessionKind.CLIENT -> "Starting"
        state is IperfSessionState.Running && state.kind == IperfSessionKind.CLIENT -> "Running"
        state is IperfSessionState.Stopping && state.kind == IperfSessionKind.CLIENT -> "Stopping"
        state is IperfSessionState.Stopped && state.kind == IperfSessionKind.CLIENT -> "Stopped"
        state is IperfSessionState.Finished && state.kind == IperfSessionKind.CLIENT -> "Finished"
        state is IperfSessionState.Failed && state.kind == IperfSessionKind.CLIENT -> "Failed"
        else -> "Idle"
    }
}

private fun failureMessage(state: IperfSessionState): String? {
    return (state as? IperfSessionState.Failed)
        ?.takeIf { it.kind == IperfSessionKind.CLIENT }
        ?.failure
        ?.message
}
