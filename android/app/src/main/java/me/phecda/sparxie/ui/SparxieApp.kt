package me.phecda.sparxie.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import me.phecda.sparxie.R
import me.phecda.sparxie.ui.navigation.ClientHome
import me.phecda.sparxie.ui.navigation.AppNavDisplay
import me.phecda.sparxie.ui.navigation.Licenses
import me.phecda.sparxie.ui.navigation.MoreHome
import me.phecda.sparxie.ui.navigation.Navigator
import me.phecda.sparxie.ui.navigation.ServerBindAddress
import me.phecda.sparxie.ui.navigation.ServerHome
import me.phecda.sparxie.ui.navigation.Settings
import me.phecda.sparxie.ui.navigation.TopLevelRouteMetadataKey
import me.phecda.sparxie.ui.navigation.rememberNavigationState
import me.phecda.sparxie.ui.screens.client.ClientRoute
import me.phecda.sparxie.ui.screens.client.ClientToolbarAction
import me.phecda.sparxie.ui.screens.client.ClientViewModel
import me.phecda.sparxie.ui.screens.licenses.LicensesScreen
import me.phecda.sparxie.ui.screens.licenses.LicensesTopBarState
import me.phecda.sparxie.ui.screens.more.MoreScreen
import me.phecda.sparxie.ui.screens.server.ServerBindAddressScreen
import me.phecda.sparxie.ui.screens.server.ServerRoute
import me.phecda.sparxie.ui.screens.server.ServerToolbarAction
import me.phecda.sparxie.ui.screens.server.ServerViewModel
import me.phecda.sparxie.ui.screens.settings.SettingsScreen

private data class TopLevelDestination(
    val route: NavKey,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(ClientHome, R.string.nav_client, Icons.Default.Devices),
    TopLevelDestination(ServerHome, R.string.nav_server, Icons.Default.Dns),
    TopLevelDestination(MoreHome, R.string.nav_more, Icons.Default.MoreHoriz),
)

private val topLevelRoutes: Set<NavKey> = topLevelDestinations
    .mapTo(linkedSetOf()) { it.route }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparxieApp() {
    val clientViewModel: ClientViewModel = viewModel()
    val serverViewModel: ServerViewModel = viewModel()
    val navigationState = rememberNavigationState(
        startRoute = ClientHome,
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember { Navigator(navigationState) }
    val licensesTopBarState = remember { LicensesTopBarState() }
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]
        ?.lastOrNull()
    val onBack: () -> Unit = {
        if (currentRoute == Licenses && licensesTopBarState.canNavigateBack) {
            licensesTopBarState.navigateBack()
        } else {
            navigator.goBack()
        }
    }
    val title = when (currentRoute) {
        ClientHome -> stringResource(R.string.nav_client)
        ServerHome -> stringResource(R.string.nav_server)
        ServerBindAddress -> stringResource(R.string.title_bind_address)
        MoreHome -> stringResource(R.string.nav_more)
        Settings -> stringResource(R.string.title_settings)
        Licenses -> licensesTopBarState.title
            ?: stringResource(R.string.title_open_source_licenses)
        else -> stringResource(R.string.app_name)
    }
    val isChildPage = currentRoute !in topLevelRoutes
    val entryProvider = entryProvider<NavKey> {
        entry<ClientHome>(metadata = metadata {
            put(TopLevelRouteMetadataKey, ClientHome as NavKey)
        }) {
            ClientRoute(viewModel = clientViewModel)
        }
        entry<ServerHome>(metadata = metadata {
            put(TopLevelRouteMetadataKey, ServerHome as NavKey)
        }) {
            ServerRoute(viewModel = serverViewModel)
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
            LicensesScreen(topBarState = licensesTopBarState)
        }
    }

    NavigationSuiteScaffold(
        navigationItems = {
            topLevelDestinations.forEach { destination ->
                NavigationSuiteItem(
                    selected = navigationState.topLevelRoute == destination.route,
                    onClick = { navigator.navigate(destination.route) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    actions = {
                        when (currentRoute) {
                            ClientHome -> ClientToolbarAction(viewModel = clientViewModel)
                            ServerHome -> ServerToolbarAction(viewModel = serverViewModel)
                        }
                    },
                    navigationIcon = {
                        if (isChildPage) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            AppNavDisplay(
                navigationState = navigationState,
                entryProvider = entryProvider,
                topLevelRoutes = topLevelRoutes,
                contentPadding = innerPadding,
                onBack = onBack,
            )
        }
    }
}
