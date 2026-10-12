package com.tirepressure.app.ui.navigation

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.createGraph
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.bikes.BikeEditScreen
import com.tirepressure.app.ui.info.AppInfoScreen
import com.tirepressure.app.ui.onboarding.OnboardingScreen
import com.tirepressure.app.ui.overview.OverviewScreen
import com.tirepressure.app.ui.overview.OverviewViewModel
import com.tirepressure.app.ui.settings.SettingsScreen
import com.tirepressure.app.ui.settings.SettingsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    overviewViewModel: OverviewViewModel,
    settingsViewModel: SettingsViewModel,
    bikeRepository: BikeRepository,
    preferencesRepository: PreferencesRepository,
    isOnboardingCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isDualPaneSupported = (configuration.smallestScreenWidthDp >= 600 && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) || configuration.screenWidthDp >= 840

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDetailRoute = isDualPaneSupported && isOnboardingCompleted && currentRoute != null && currentRoute != Screen.Overview.route && currentRoute != Screen.Onboarding.route

    val startDestination = if (isOnboardingCompleted) Screen.Overview.route else Screen.Onboarding.route

    // Create persistent NavGraph shared across configurations
    val navGraph = remember(navController, startDestination) {
        navController.createGraph(startDestination = startDestination) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    preferencesRepository = preferencesRepository,
                    onOnboardingFinished = {
                        navController.navigate(Screen.Overview.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Overview.route) {
                OverviewScreen(
                    viewModel = overviewViewModel,
                    onNavigateToAddBike = {
                        navController.navigate(Screen.BikeEdit.createRoute("new"))
                    },
                    onNavigateToEditBike = { bikeId ->
                        navController.navigate(Screen.BikeEdit.createRoute(bikeId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToAppInfo = {
                        navController.navigate(Screen.AppInfo.route)
                    }
                )
            }

            composable(Screen.BikeEdit.route) { backStackEntry ->
                val bikeId = backStackEntry.arguments?.getString("bikeId") ?: "new"
                BikeEditScreen(
                    bikeId = bikeId,
                    bikeRepository = bikeRepository,
                    preferencesRepository = preferencesRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AppInfo.route) {
                AppInfoScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    LaunchedEffect(navGraph) {
        if (navController.currentDestination == null) {
            navController.graph = navGraph
        }
    }

    // Keep track of the last detail route and args so exit animations render real content instead of popping to blank
    var lastDetailRoute by remember { mutableStateOf<String?>(null) }
    var lastDetailBikeId by remember { mutableStateOf("new") }

    LaunchedEffect(currentRoute, navBackStackEntry) {
        if (isDetailRoute) {
            lastDetailRoute = currentRoute
            if (currentRoute == Screen.BikeEdit.route) {
                lastDetailBikeId = navBackStackEntry?.arguments?.getString("bikeId") ?: "new"
            }
        }
    }

    if (isDualPaneSupported && isOnboardingCompleted) {
        BackHandler(enabled = isDetailRoute) {
            navController.popBackStack()
        }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val totalWidth = maxWidth
            val halfWidth = totalWidth / 2

            val detailProgress by animateFloatAsState(
                targetValue = if (isDetailRoute) 1f else 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "DualPaneWidthProgress"
            )

            val leftPaneWidth = totalWidth - (halfWidth * detailProgress)

            // Left Master Pane: Overview (fluidly animated from 100% to 50%)
            Box(
                modifier = Modifier
                    .width(leftPaneWidth)
                    .fillMaxHeight()
            ) {
                OverviewScreen(
                    viewModel = overviewViewModel,
                    onNavigateToAddBike = {
                        navController.navigate(Screen.BikeEdit.createRoute("new"))
                    },
                    onNavigateToEditBike = { bikeId ->
                        navController.navigate(Screen.BikeEdit.createRoute(bikeId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToAppInfo = {
                        navController.navigate(Screen.AppInfo.route)
                    }
                )
            }

            // Right Detail Pane: Slides in from the right edge with 0 saccades
            if (isDetailRoute || detailProgress > 0.001f) {
                val dividerAlpha by animateFloatAsState(
                    targetValue = if (isDetailRoute) 0.25f else 0.0f,
                    animationSpec = tween(300),
                    label = "DividerAlpha"
                )

                Box(
                    modifier = Modifier
                        .offset(x = leftPaneWidth)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = dividerAlpha))
                )

                val rightPaneOffsetX = halfWidth * (1f - detailProgress)
                Box(
                    modifier = Modifier
                        .offset(x = halfWidth + rightPaneOffsetX)
                        .width(halfWidth)
                        .fillMaxHeight()
                        .clipToBounds()
                ) {
                    val activeRoute = if (isDetailRoute) currentRoute else lastDetailRoute
                    val activeBikeId = if (isDetailRoute) {
                        navBackStackEntry?.arguments?.getString("bikeId") ?: lastDetailBikeId
                    } else {
                        lastDetailBikeId
                    }

                    AnimatedContent(
                        targetState = activeRoute to activeBikeId,
                        transitionSpec = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(250)) togetherWith
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(200))
                        },
                        label = "DetailScreenTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { (route, bikeId) ->
                        when (route) {
                            Screen.Settings.route -> {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            Screen.AppInfo.route -> {
                                AppInfoScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            Screen.BikeEdit.route -> {
                                BikeEditScreen(
                                    bikeId = bikeId,
                                    bikeRepository = bikeRepository,
                                    preferencesRepository = preferencesRepository,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    } else {
        // Single pane (Phone portrait / Foldable folded / First-time onboarding)
        NavHost(
            navController = navController,
            graph = navGraph,
            modifier = modifier.fillMaxSize(),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            }
        )
    }
}
