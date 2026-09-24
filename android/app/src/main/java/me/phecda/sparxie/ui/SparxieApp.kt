package me.phecda.sparxie.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import me.phecda.sparxie.ui.navigation.Routes
import me.phecda.sparxie.ui.screens.ClientScreen
import me.phecda.sparxie.ui.screens.LicenseDetailScreen
import me.phecda.sparxie.ui.screens.LicensesScreen
import me.phecda.sparxie.ui.screens.MoreScreen
import me.phecda.sparxie.ui.screens.ServerBindAddressScreen
import me.phecda.sparxie.ui.screens.ServerScreen
import me.phecda.sparxie.ui.screens.SettingsScreen

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.ClientGraph, "Client", Icons.Default.Devices),
    TopLevelDestination(Routes.ServerGraph, "Server", Icons.Default.Dns),
    TopLevelDestination(Routes.MoreGraph, "More", Icons.Default.MoreHoriz),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparxieApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val currentTopLevel = topLevelDestinations.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }
    val title = when (currentRoute) {
        Routes.ClientHome -> "Client"
        Routes.ServerHome -> "Server"
        Routes.ServerBindAddress -> "Bind Address"
        Routes.MoreHome -> "More"
        Routes.Settings -> "Settings"
        Routes.Licenses -> "Open Source Licenses"
        Routes.LicenseDetail -> backStackEntry?.arguments
            ?.getString(Routes.LicenseIdArgument)
            .orEmpty()
        else -> currentTopLevel?.label ?: "Sparxie"
    }
    val isChildPage = currentRoute !in setOf(
        Routes.ClientHome,
        Routes.ServerHome,
        Routes.MoreHome,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (isChildPage) {
                        IconButton(onClick = { navController.navigateUp() }) {
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
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                            }
                        },
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
        AppNavHost(
            navController = navController,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.ClientGraph,
        modifier = Modifier.padding(contentPadding),
    ) {
        navigation(
            startDestination = Routes.ClientHome,
            route = Routes.ClientGraph,
        ) {
            composable(Routes.ClientHome) {
                ClientScreen()
            }
        }

        navigation(
            startDestination = Routes.ServerHome,
            route = Routes.ServerGraph,
        ) {
            composable(Routes.ServerHome) {
                ServerScreen(
                    onBindAddressClick = {
                        navController.navigate(Routes.ServerBindAddress)
                    },
                )
            }
            composable(Routes.ServerBindAddress) {
                ServerBindAddressScreen()
            }
        }

        navigation(
            startDestination = Routes.MoreHome,
            route = Routes.MoreGraph,
        ) {
            composable(Routes.MoreHome) {
                MoreScreen(
                    onSettingsClick = { navController.navigate(Routes.Settings) },
                    onLicensesClick = { navController.navigate(Routes.Licenses) },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen()
            }
            composable(Routes.Licenses) {
                LicensesScreen()
            }
            composable(
                route = Routes.LicenseDetail,
                arguments = listOf(
                    navArgument(Routes.LicenseIdArgument) {
                        type = NavType.StringType
                    },
                ),
            ) { entry ->
                LicenseDetailScreen(
                    licenseId = entry.arguments?.getString(Routes.LicenseIdArgument).orEmpty(),
                )
            }
        }
    }
}
