package me.phecda.sparxie.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MoreScreen(
    onSettingsClick: () -> Unit,
    onLicensesClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSettingsClick),
            headlineContent = { Text("Settings") },
            supportingContent = { Text("App preferences") },
            leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
        )
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLicensesClick),
            headlineContent = { Text("Open Source Licenses") },
            supportingContent = { Text("Dependency licenses") },
            leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
        )
    }
}
