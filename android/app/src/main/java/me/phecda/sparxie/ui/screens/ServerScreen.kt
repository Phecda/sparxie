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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.phecda.sparxie.R
import me.phecda.sparxie.runtime.IperfSessionKind
import me.phecda.sparxie.runtime.IperfSessionState

@Composable
fun ServerRoute(viewModel: ServerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ServerScreen(
        state = uiState,
        onServerPortChange = viewModel::onServerPortChange,
        onToggleJsonEvent = viewModel::onToggleJsonEvent,
    )
}

@Composable
fun ServerToolbarAction(viewModel: ServerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState = uiState.sessionState
    val isServerActive = isServerActive(sessionState)
    val isStopping = sessionState is IperfSessionState.Stopping &&
        sessionState.kind == IperfSessionKind.SERVER
    val onStart = rememberLocalNetworkStart(
        onStart = viewModel::onStart,
        onPermissionDenied = viewModel::onLocalNetworkPermissionDenied,
    )

    FilledIconButton(
        onClick = if (isServerActive) viewModel::onStop else onStart,
        enabled = !isStopping,
    ) {
        Icon(
            imageVector = if (isServerActive) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = stringResource(
                if (isServerActive) R.string.action_stop_server else R.string.action_start_server,
            ),
        )
    }
}

@Composable
fun ServerScreen(
    state: ServerUiState,
    onServerPortChange: (String) -> Unit,
    onToggleJsonEvent: (Long) -> Unit,
) {
    val sessionState = state.sessionState
    val isServerActive = isServerActive(sessionState)
    val errorMessage = state.actionErrorRes?.let { stringResource(it) }
        ?: failureMessage(sessionState)
        ?: "-"

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
                label = { Text(stringResource(R.string.field_server_port)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.section_status))
                Text(stringResource(statusText(sessionState)))
                Text(stringResource(R.string.section_error))
                Text(errorMessage)
            }
        }

        item {
            Text(stringResource(R.string.section_live_data))
        }

        if (state.jsonEvents.isEmpty()) {
            item {
                Text(stringResource(R.string.message_no_json_events))
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
                        headlineContent = {
                            Text(event.name ?: stringResource(R.string.event_fallback))
                        },
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

private fun statusText(state: IperfSessionState): Int {
    return when {
        state is IperfSessionState.Starting && state.kind == IperfSessionKind.SERVER -> R.string.status_starting
        state is IperfSessionState.Running && state.kind == IperfSessionKind.SERVER -> R.string.status_listening
        state is IperfSessionState.Stopping && state.kind == IperfSessionKind.SERVER -> R.string.status_stopping
        state is IperfSessionState.Stopped && state.kind == IperfSessionKind.SERVER -> R.string.status_stopped
        state is IperfSessionState.Finished && state.kind == IperfSessionKind.SERVER -> R.string.status_finished
        state is IperfSessionState.Failed && state.kind == IperfSessionKind.SERVER -> R.string.status_failed
        else -> R.string.status_idle
    }
}

@Composable
private fun failureMessage(state: IperfSessionState): String? {
    val failure = (state as? IperfSessionState.Failed)
        ?.takeIf { it.kind == IperfSessionKind.SERVER }
        ?.failure
        ?: return null

    return failure.nativeMessage ?: stringResource(failure.fallbackMessageRes)
}
