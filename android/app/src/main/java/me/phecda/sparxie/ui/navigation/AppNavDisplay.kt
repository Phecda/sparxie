package me.phecda.sparxie.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

internal object TopLevelRouteMetadataKey : NavMetadataKey<NavKey>

@Composable
internal fun AppNavDisplay(
    navigationState: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    topLevelRoutes: Set<NavKey>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    NavDisplay(
        entries = navigationState.toDecoratedEntries(entryProvider),
        onBack = onBack,
        modifier = Modifier.padding(contentPadding),
        transitionSpec = {
            if (isTopLevelTransition(topLevelRoutes)) {
                topLevelTransition()
            } else {
                forwardTransition()
            }
        },
        popTransitionSpec = {
            if (isTopLevelTransition(topLevelRoutes)) {
                topLevelPopTransition()
            } else {
                popTransition()
            }
        },
        predictivePopTransitionSpec = { _ ->
            if (isTopLevelTransition(topLevelRoutes)) {
                topLevelPopTransition()
            } else {
                popTransition()
            }
        },
    )
}

private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.isTopLevelTransition(
    topLevelRoutes: Set<NavKey>,
): Boolean {
    val initialRoute = initialState.entries.lastOrNull()
        ?.metadata
        ?.get(TopLevelRouteMetadataKey.toString())
    val targetRoute = targetState.entries.lastOrNull()
        ?.metadata
        ?.get(TopLevelRouteMetadataKey.toString())
    return initialRoute is NavKey &&
        targetRoute is NavKey &&
        initialRoute in topLevelRoutes &&
        targetRoute in topLevelRoutes &&
        initialRoute != targetRoute
}

private fun topLevelTransition(): ContentTransform =
    (fadeIn(animationSpec = tween(220)) +
        scaleIn(initialScale = 0.98f, animationSpec = tween(220))) togetherWith
        (fadeOut(animationSpec = tween(220)) +
            scaleOut(targetScale = 1.02f, animationSpec = tween(220)))

private fun topLevelPopTransition(): ContentTransform =
    topLevelTransition()

private fun forwardTransition(): ContentTransform =
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(280),
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(280),
    )

private fun popTransition(): ContentTransform =
    slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = tween(280),
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(280),
    )
