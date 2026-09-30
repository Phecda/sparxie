package me.phecda.sparxie.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.phecda.sparxie.R

@Composable
fun SettingsScreen() {
    EmptyState(
        title = stringResource(R.string.title_settings),
        message = stringResource(R.string.message_settings_unavailable),
    )
}
