package com.slaviboy.e_commerce_app_ui_compose.composable

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.slaviboy.e_commerce_app_ui_compose.composable.destinations.DetailScreenDestination
import com.slaviboy.e_commerce_app_ui_compose.util.StaticMethods.NAVIGATION_TRANSITION_DELAY
import com.slaviboy.e_commerce_app_ui_compose.util.StaticMethods.NAVIGATION_TRANSITION_DURATION


object HomeScreenTransition : DestinationStyle.Animated {

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition(): EnterTransition? {

        return when (initialState.navDestination) {
            DetailScreenDestination ->
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(NAVIGATION_TRANSITION_DURATION, NAVIGATION_TRANSITION_DELAY)
                )
            else -> null
        }
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition(): ExitTransition? {

        return when (targetState.navDestination) {
            DetailScreenDestination ->
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(NAVIGATION_TRANSITION_DURATION, NAVIGATION_TRANSITION_DELAY)
                )
            else -> null
        }
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition(): EnterTransition? {

        return when (initialState.navDestination) {
            DetailScreenDestination ->
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(NAVIGATION_TRANSITION_DURATION, NAVIGATION_TRANSITION_DELAY)
                )
            else -> null
        }
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition(): ExitTransition? {

        return when (targetState.navDestination) {
            DetailScreenDestination ->
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(NAVIGATION_TRANSITION_DURATION, NAVIGATION_TRANSITION_DELAY)
                )
            else -> null
        }
    }
}