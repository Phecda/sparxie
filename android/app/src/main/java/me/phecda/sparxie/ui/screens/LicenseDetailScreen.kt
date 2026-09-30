package me.phecda.sparxie.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.phecda.sparxie.R

@Composable
fun LicenseDetailScreen(licenseId: String) {
    EmptyState(
        title = licenseId,
        message = stringResource(R.string.message_license_details_unavailable),
    )
}
