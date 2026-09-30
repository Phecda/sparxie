package me.phecda.sparxie.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.phecda.sparxie.R

@Composable
fun LicensesScreen() {
    EmptyState(
        title = stringResource(R.string.title_open_source_licenses),
        message = stringResource(R.string.message_licenses_unavailable),
    )
}
