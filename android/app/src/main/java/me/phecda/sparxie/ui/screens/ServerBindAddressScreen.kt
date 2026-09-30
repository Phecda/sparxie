package me.phecda.sparxie.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.phecda.sparxie.R

@Composable
fun ServerBindAddressScreen() {
    EmptyState(
        title = stringResource(R.string.title_bind_address),
        message = stringResource(R.string.message_bind_address_unavailable),
    )
}
