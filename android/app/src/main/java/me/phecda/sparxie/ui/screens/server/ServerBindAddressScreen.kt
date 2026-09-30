package me.phecda.sparxie.ui.screens.server

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.phecda.sparxie.R
import me.phecda.sparxie.ui.components.EmptyState

@Composable
fun ServerBindAddressScreen() {
    EmptyState(
        title = stringResource(R.string.title_bind_address),
        message = stringResource(R.string.message_bind_address_unavailable),
    )
}
