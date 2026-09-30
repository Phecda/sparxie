package me.phecda.sparxie.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LicensesScreen(onLicenseClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = licenseEntries,
            key = { it.id },
        ) { license ->
            ListItem(
                modifier = Modifier.clickable { onLicenseClick(license.id) },
                headlineContent = { Text(license.name) },
                supportingContent = { Text(license.licenseName) },
            )
            HorizontalDivider()
        }
    }
}
