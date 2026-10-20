package com.tirepressure.app.ui.navigation

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
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.bikes.BikeEditScreen
import com.tirepressure.app.ui.bikes.BikeEditViewModel
import com.tirepressure.app.ui.info.AppInfoScreen
import com.tirepressure.app.ui.onboarding.OnboardingScreen
import com.tirepressure.app.ui.overview.OverviewScreen
import com.tirepressure.app.ui.overview.OverviewViewModel
import com.tirepressure.app.ui.settings.SettingsScreen
import com.tirepressure.app.ui.settings.SettingsViewModel

@Composable
fun AppNavigation(
    overviewViewModel: OverviewViewModel,
    settingsViewModel: SettingsViewModel,
    bikeEditViewModel: BikeEditViewModel,
    bikeRepository: BikeRepository,
    preferencesRepository: PreferencesRepository,
    isOnboardingCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val reactiveState = rememberReactiveScreenState()
    val isDualPaneSupported = reactiveState.isDualPaneSupported

    val backStack = rememberNavBackStack(
        if (isOnboardingCompleted) Screen.Overview else Screen.Onboarding
    )

    val currentScreen: Screen = (backStack.lastOrNull() as? Screen) ?: Screen.Overview
    val isDetailRoute = isDualPaneSupported &&
        isOnboardingCompleted &&
        currentScreen !is Screen.Overview &&
        currentScreen !is Screen.Onboarding

    // Garde une trace du dernier écran de détail pour les transitions animées de fermeture
    var lastDetailScreen by remember { mutableStateOf<Screen?>(null) }
    LaunchedEffect(currentScreen) {
        if (isDetailRoute) {
            lastDetailScreen = currentScreen
        }
    }

    if (isDualPaneSupported && isOnboardingCompleted) {
        BackHandler(enabled = isDetailRoute) {
            if (backStack.size > 1) {
                if (currentScreen is Screen.BikeEdit) {
                    bikeEditViewModel.reset()
                }
                backStack.removeAt(backStack.size - 1)
            }
        }

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .displayCutoutPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val totalWidth = maxWidth
            val halfWidth = totalWidth / 2

            val detailProgress by animateFloatAsState(
                targetValue = if (isDetailRoute) 1f else 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "DualPaneWidthProgress"
            )

            val leftPaneWidth = totalWidth - (halfWidth * detailProgress)

            // Volet Maître Gauche : Aperçu (animé fluidement de 100% à 50%)
            Box(
                modifier = Modifier
                    .width(leftPaneWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                OverviewScreen(
                    viewModel = overviewViewModel,
                    isDualPane = true,
                    onNavigateToAddBike = {
                        backStack.add(Screen.BikeEdit("new"))
                    },
                    onNavigateToEditBike = { bikeId ->
                        backStack.add(Screen.BikeEdit(bikeId))
                    },
                    onNavigateToSettings = {
                        backStack.add(Screen.Settings)
                    },
                    onNavigateToAppInfo = {
                        backStack.add(Screen.AppInfo)
                    }
                )
            }

            // Volet Détail Droit : Glisse depuis la droite sans saccade ni clignotement
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
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val activeDetail = if (isDetailRoute) currentScreen else lastDetailScreen

                    AnimatedContent(
                        targetState = activeDetail,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                        },
                        label = "DetailScreenTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) { targetScreen ->
                        when (targetScreen) {
                            is Screen.Settings -> {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateBack = {
                                        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                    }
                                )
                            }
                            is Screen.AppInfo -> {
                                AppInfoScreen(
                                    onNavigateBack = {
                                        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                    }
                                )
                            }
                            is Screen.BikeEdit -> {
                                BikeEditScreen(
                                    bikeId = targetScreen.bikeId,
                                    viewModel = bikeEditViewModel,
                                    preferencesRepository = preferencesRepository,
                                    onNavigateBack = {
                                        bikeEditViewModel.reset()
                                        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                    }
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    } else {
        // Mode volet unique (Smartphone portrait / Pliable plié / Premier onboarding)
        NavDisplay(
            backStack = backStack,
            modifier = modifier.fillMaxSize(),
            entryProvider = entryProvider {
                entry<Screen.Onboarding> {
                    OnboardingScreen(
                        preferencesRepository = preferencesRepository,
                        onOnboardingFinished = {
                            backStack.clear()
                            backStack.add(Screen.Overview)
                        }
                    )
                }
                entry<Screen.Overview> {
                    OverviewScreen(
                        viewModel = overviewViewModel,
                        onNavigateToAddBike = {
                            backStack.add(Screen.BikeEdit("new"))
                        },
                        onNavigateToEditBike = { bikeId ->
                            backStack.add(Screen.BikeEdit(bikeId))
                        },
                        onNavigateToSettings = {
                            backStack.add(Screen.Settings)
                        },
                        onNavigateToAppInfo = {
                            backStack.add(Screen.AppInfo)
                        }
                    )
                }
                entry<Screen.BikeEdit> { key ->
                    BikeEditScreen(
                        bikeId = key.bikeId,
                        viewModel = bikeEditViewModel,
                        preferencesRepository = preferencesRepository,
                        onNavigateBack = {
                            bikeEditViewModel.reset()
                            if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                        }
                    )
                }
                entry<Screen.Settings> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                        }
                    )
                }
                entry<Screen.AppInfo> {
                    AppInfoScreen(
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                        }
                    )
                }
            }
        )
    }
}
