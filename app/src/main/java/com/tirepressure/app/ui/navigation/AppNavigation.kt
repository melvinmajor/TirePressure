package com.tirepressure.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SceneStrategy
import io.github.angelomarzocchi.flight.FlightHost
import io.github.angelomarzocchi.flight.FlightNavDisplay
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = calculatePaneScaffoldDirective(windowAdaptiveInfo)
    val listDetailStrategy: SceneStrategy<NavKey> = rememberListDetailSceneStrategy(
        directive = directive
    )

    val backStack = rememberNavBackStack(
        if (isOnboardingCompleted) Screen.Overview else Screen.Onboarding(1)
    )

    LaunchedEffect(isOnboardingCompleted) {
        if (!isOnboardingCompleted && backStack.lastOrNull() !is Screen.Onboarding) {
            backStack.clear()
            backStack.add(Screen.Onboarding(1))
        }
    }

    val currentScreen: Screen = (backStack.lastOrNull() as? Screen) ?: Screen.Overview
    val isScaffoldDualPane = directive.maxHorizontalPartitions > 1
    val isDetailShowing = isScaffoldDualPane && currentScreen !is Screen.Overview && currentScreen !is Screen.Onboarding

    FlightHost(modifier = modifier.fillMaxSize()) {
        FlightNavDisplay(
            backStack = backStack,
            sceneStrategies = listOf(listDetailStrategy),
            modifier = Modifier.fillMaxSize(),
            entryProvider = entryProvider {
                entry<Screen.Onboarding> { key ->
                    OnboardingScreen(
                        step = key.step,
                        preferencesRepository = preferencesRepository,
                        onNavigateToStep2 = {
                            backStack.add(Screen.Onboarding(2))
                        },
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                        },
                        onOnboardingFinished = {
                            backStack.clear()
                            backStack.add(Screen.Overview)
                        }
                    )
                }
                entry<Screen.Overview>(
                    metadata = mapOf(
                        "androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole" to ListDetailPaneScaffoldRole.List
                    )
                ) {
                    OverviewScreen(
                        viewModel = overviewViewModel,
                        isDualPane = isDetailShowing,
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
                entry<Screen.BikeEdit>(
                    metadata = mapOf(
                        "androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole" to ListDetailPaneScaffoldRole.Detail
                    )
                ) { key ->
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
                entry<Screen.Settings>(
                    metadata = mapOf(
                        "androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole" to ListDetailPaneScaffoldRole.Detail
                    )
                ) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                        },
                        onNavigateToOnboarding = {
                            backStack.clear()
                            backStack.add(Screen.Onboarding(1))
                        }
                    )
                }
                entry<Screen.AppInfo>(
                    metadata = mapOf(
                        "androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole" to ListDetailPaneScaffoldRole.Detail
                    )
                ) {
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
