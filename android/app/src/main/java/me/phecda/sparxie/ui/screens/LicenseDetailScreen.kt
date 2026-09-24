package me.phecda.sparxie.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun LicenseDetailScreen(licenseId: String) {
    EmptyState(
        title = licenseId,
        message = "License details will be available in a later stage.",
    )
}
