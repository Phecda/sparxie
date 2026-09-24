package me.phecda.sparxie.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import me.phecda.sparxie.ui.navigation.ClientHome
import me.phecda.sparxie.ui.navigation.AppNavDisplay
import me.phecda.sparxie.ui.navigation.LicenseDetail
import me.phecda.sparxie.ui.navigation.Licenses
import me.phecda.sparxie.ui.navigation.MoreHome
import me.phecda.sparxie.ui.navigation.Navigator
import me.phecda.sparxie.ui.navigation.ServerBindAddress
import me.phecda.sparxie.ui.navigation.ServerHome
import me.phecda.sparxie.ui.navigation.Settings
import me.phecda.sparxie.ui.navigation.TopLevelRouteMetadataKey
import me.phecda.sparxie.ui.navigation.rememberNavigationState
import me.phecda.sparxie.ui.screens.ClientScreen
import me.phecda.sparxie.ui.screens.LicenseDetailScreen
import me.phecda.sparxie.ui.screens.LicensesScreen
import me.phecda.sparxie.ui.screens.MoreScreen
import me.phecda.sparxie.ui.screens.ServerBindAddressScreen
import me.phecda.sparxie.ui.screens.ServerScreen
import me.phecda.sparxie.ui.screens.SettingsScreen

private data class TopLevelDestination(
    val route: NavKey,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(ClientHome, "Client", Icons.Default.Devices),
    TopLevelDestination(ServerHome, "Server", Icons.Default.Dns),
    TopLevelDestination(MoreHome, "More", Icons.Default.MoreHoriz),
)

private val topLevelRoutes: Set<NavKey> = topLevelDestinations
    .mapTo(linkedSetOf()) { it.route }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparxieApp() {
    val navigationState = rememberNavigationState(
        startRoute = ClientHome,
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember { Navigator(navigationState) }
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]
        ?.lastOrNull()
    val title = when (val route = currentRoute) {
        ClientHome -> "Client"
        ServerHome -> "Server"
        ServerBindAddress -> "Bind Address"
        MoreHome -> "More"
        Settings -> "Settings"
        Licenses -> "Open Source Licenses"
        is LicenseDetail -> route.licenseId
        else -> "Sparxie"
    }
    val isChildPage = currentRoute !in topLevelRoutes
    val entryProvider = entryProvider<NavKey> {
        entry<ClientHome>(metadata = metadata {
            put(TopLevelRouteMetadataKey, ClientHome as NavKey)
        }) {
            ClientScreen()
        }
        entry<ServerHome>(metadata = metadata {
            put(TopLevelRouteMetadataKey, ServerHome as NavKey)
        }) {
            ServerScreen(
                onBindAddressClick = { navigator.navigate(ServerBindAddress) },
            )
        }
        entry<ServerBindAddress>(metadata = metadata {
            put(TopLevelRouteMetadataKey, ServerHome as NavKey)
        }) {
            ServerBindAddressScreen()
        }
        entry<MoreHome>(metadata = metadata {
            put(TopLevelRouteMetadataKey, MoreHome as NavKey)
        }) {
            MoreScreen(
                onSettingsClick = { navigator.navigate(Settings) },
                onLicensesClick = { navigator.navigate(Licenses) },
            )
        }
        entry<Settings>(metadata = metadata {
            put(TopLevelRouteMetadataKey, MoreHome as NavKey)
        }) {
            SettingsScreen()
        }
        entry<Licenses>(metadata = metadata {
            put(TopLevelRouteMetadataKey, MoreHome as NavKey)
        }) {
            LicensesScreen()
        }
        entry<LicenseDetail>(metadata = metadata {
            put(TopLevelRouteMetadataKey, MoreHome as NavKey)
        }) { route ->
            LicenseDetailScreen(licenseId = route.licenseId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (isChildPage) {
                        IconButton(onClick = navigator::goBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = navigationState.topLevelRoute == destination.route,
                        onClick = { navigator.navigate(destination.route) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AppNavDisplay(
            navigationState = navigationState,
            entryProvider = entryProvider,
            topLevelRoutes = topLevelRoutes,
            contentPadding = innerPadding,
            onBack = navigator::goBack,
        )
    }
}
