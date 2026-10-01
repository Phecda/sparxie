package me.phecda.sparxie.ui.screens.licenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Stable
class LicensesTopBarState {
    var title by mutableStateOf<String?>(null)
    var canNavigateBack by mutableStateOf(false)
    var navigateBack: () -> Unit = {}

    fun clear() {
        title = null
        canNavigateBack = false
        navigateBack = {}
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LicensesScreen(topBarState: LicensesTopBarState) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()
    val selectedLicenseName = navigator.currentDestination
        ?.contentKey
        ?.let { licenseId -> licenseEntries.firstOrNull { it.id == licenseId }?.name }
    val canNavigateBack = navigator.canNavigateBack()
    val onBack: () -> Unit = {
        coroutineScope.launch { navigator.navigateBack() }
    }

    SideEffect {
        topBarState.title = selectedLicenseName
        topBarState.canNavigateBack = canNavigateBack
        topBarState.navigateBack = onBack
    }

    DisposableEffect(Unit) {
        onDispose { topBarState.clear() }
    }

    BackHandler(enabled = canNavigateBack, onBack = onBack)

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                LicensesList(
                    selectedLicenseId = navigator.currentDestination?.contentKey,
                    onLicenseClick = { licenseId ->
                        coroutineScope.launch {
                            navigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = licenseId,
                            )
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination
                    ?.contentKey
                    ?.let { licenseId -> LicenseDetailScreen(licenseId = licenseId) }
            }
        },
    )
}

@Composable
private fun LicensesList(
    selectedLicenseId: String?,
    onLicenseClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(
            items = licenseEntries,
            key = { _, license -> license.id },
        ) { index, license ->
            SegmentedListItem(
                selected = license.id == selectedLicenseId,
                onClick = { onLicenseClick(license.id) },
                shapes = ListItemDefaults.segmentedShapes(
                    index = index,
                    count = licenseEntries.size,
                ),
                content = { Text(license.name) },
                supportingContent = { Text(license.licenseName) },
            )
        }
    }
}
