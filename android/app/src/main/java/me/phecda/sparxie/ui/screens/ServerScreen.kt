package me.phecda.sparxie.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
fun ServerRoute(viewModel: ServerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onStart = rememberLocalNetworkStart(
        onStart = viewModel::onStart,
        onPermissionDenied = viewModel::onLocalNetworkPermissionDenied,
    )

    ServerScreen(
        state = uiState,
        onServerPortChange = viewModel::onServerPortChange,
        onStart = onStart,
        onStop = viewModel::onStop,
        onToggleJsonEvent = viewModel::onToggleJsonEvent,
    )
}

@Composable
fun ServerScreen(
    state: ServerUiState,
    onServerPortChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onToggleJsonEvent: (Long) -> Unit,
) {
    val sessionState = state.sessionState
    val isServerActive = isServerActive(sessionState)
    val isStopping = sessionState is IperfSessionState.Stopping &&
        sessionState.kind == IperfSessionKind.SERVER
    val errorMessage = state.actionError ?: failureMessage(sessionState) ?: "-"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.portInput,
                onValueChange = onServerPortChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isServerActive,
                label = { Text("Server Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        item {
            Button(
                onClick = if (isServerActive) onStop else onStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isStopping,
            ) {
                Icon(
                    imageVector = if (isServerActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isServerActive) "Stop Server" else "Start Server")
            }
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

private fun isServerActive(state: IperfSessionState): Boolean {
    return when (state) {
        is IperfSessionState.Starting -> state.kind == IperfSessionKind.SERVER
        is IperfSessionState.Running -> state.kind == IperfSessionKind.SERVER
        is IperfSessionState.Stopping -> state.kind == IperfSessionKind.SERVER
        else -> false
    }
}

private fun statusText(state: IperfSessionState): String {
    return when {
        state is IperfSessionState.Starting && state.kind == IperfSessionKind.SERVER -> "Starting"
        state is IperfSessionState.Running && state.kind == IperfSessionKind.SERVER -> "Listening"
        state is IperfSessionState.Stopping && state.kind == IperfSessionKind.SERVER -> "Stopping"
        state is IperfSessionState.Stopped && state.kind == IperfSessionKind.SERVER -> "Stopped"
        state is IperfSessionState.Finished && state.kind == IperfSessionKind.SERVER -> "Finished"
        state is IperfSessionState.Failed && state.kind == IperfSessionKind.SERVER -> "Failed"
        else -> "Idle"
    }
}

private fun failureMessage(state: IperfSessionState): String? {
    return (state as? IperfSessionState.Failed)
        ?.takeIf { it.kind == IperfSessionKind.SERVER }
        ?.failure
        ?.message
}
