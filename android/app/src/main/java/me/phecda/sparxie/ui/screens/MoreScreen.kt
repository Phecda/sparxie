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
import androidx.compose.ui.res.stringResource
import me.phecda.sparxie.R

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
            headlineContent = { Text(stringResource(R.string.title_settings)) },
            supportingContent = { Text(stringResource(R.string.more_settings_summary)) },
            leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
        )
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLicensesClick),
            headlineContent = { Text(stringResource(R.string.title_open_source_licenses)) },
            supportingContent = { Text(stringResource(R.string.more_licenses_summary)) },
            leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
        )
    }
}
