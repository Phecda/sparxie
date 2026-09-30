package me.phecda.sparxie.ui.screens.licenses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun LicenseDetailScreen(licenseId: String) {
    val license = licenseEntries.firstOrNull { it.id == licenseId }
    if (license == null) {
        Text(
            text = "License not found.",
            modifier = Modifier.padding(24.dp),
        )
        return
    }

    val context = LocalContext.current
    val licenseText = remember(license.licenseTextRes) {
        context.resources.openRawResource(license.licenseTextRes)
            .bufferedReader()
            .use { it.readText() }
    }

    Text(
        text = licenseText,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        fontFamily = FontFamily.Monospace,
    )
}
