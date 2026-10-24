package com.tirepressure.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SceneStrategy
import androidx.window.core.layout.WindowSizeClass
import com.tirepressure.app.R
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.bikes.BikeEditScreen
import com.tirepressure.app.ui.bikes.BikeEditViewModel
import com.tirepressure.app.ui.components.TirePressurePulseLogo
import com.tirepressure.app.ui.components.appGlossCardColors
import com.tirepressure.app.ui.info.AppInfoScreen
import com.tirepressure.app.ui.onboarding.OnboardingScreen
import com.tirepressure.app.ui.overview.OverviewScreen
import com.tirepressure.app.ui.overview.OverviewViewModel
import com.tirepressure.app.ui.settings.SettingsScreen
import com.tirepressure.app.ui.settings.SettingsViewModel
import io.github.angelomarzocchi.flight.FlightHost
import io.github.angelomarzocchi.flight.FlightNavDisplay
import io.github.angelomarzocchi.lumen.GlossCard

private const val OVERVIEW_LIST_DETAIL_SCENE_KEY = "overview_list_detail"

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
    // Phones in landscape have Compact height (< 480dp) even when width >= 600dp, so they must
    // stay single-pane (where OverviewScreen uses a 2-column staggered grid). Unfolded foldables
    // and tablets have at least Medium height (>= 480dp) AND Medium/Expanded width (>= 600dp),
    // activating the canonical 2-pane List-Detail layout while respecting foldable hinge bounds.
    val isCompactHeight = !windowAdaptiveInfo.windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
    )
    val directive = if (isCompactHeight) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
    } else {
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(windowAdaptiveInfo)
    }
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

    val isScaffoldDualPane = directive.maxHorizontalPartitions > 1

    fun navigateToDetail(target: Screen) {
        val currentTop = backStack.lastOrNull() as? Screen
        if (currentTop == target) return

        if (target is Screen.BikeEdit) {
            bikeEditViewModel.reset()
        }

        if (isScaffoldDualPane) {
            // In dual-pane mode, replace the active detail pane above Overview rather than stacking infinitely
            while (backStack.size > 1 && backStack.lastOrNull() !is Screen.Overview) {
                backStack.removeAt(backStack.size - 1)
            }
            backStack.add(target)
        } else {
            backStack.add(target)
        }
    }

    fun popDetail() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    // Keying FlightHost by isScaffoldDualPane ensures that when folding/unfolding a foldable or
    // resizing across the 1-pane / 2-pane threshold, PresenceDisplay cleanly disposes the previous
    // scene type before composing the new one, preventing duplicate SaveableStateProvider keys
    // when transitioning between SinglePaneScene and ListDetailScene.
    key(isScaffoldDualPane) {
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
                                if (backStack.lastOrNull() != Screen.Onboarding(2)) {
                                    backStack.add(Screen.Onboarding(2))
                                }
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
                        metadata = ListDetailSceneStrategy.listPane(
                            sceneKey = OVERVIEW_LIST_DETAIL_SCENE_KEY,
                            detailPlaceholder = {
                                OverviewDetailPlaceholder(
                                    onOpenSettings = { navigateToDetail(Screen.Settings) },
                                    onOpenAppInfo = { navigateToDetail(Screen.AppInfo) }
                                )
                            }
                        )
                    ) {
                        OverviewScreen(
                            viewModel = overviewViewModel,
                            isDualPane = isScaffoldDualPane,
                            onNavigateToAddBike = {
                                navigateToDetail(Screen.BikeEdit("new"))
                            },
                            onNavigateToEditBike = { bikeId ->
                                navigateToDetail(Screen.BikeEdit(bikeId))
                            },
                            onNavigateToSettings = {
                                navigateToDetail(Screen.Settings)
                            },
                            onNavigateToAppInfo = {
                                navigateToDetail(Screen.AppInfo)
                            }
                        )
                    }
                    entry<Screen.BikeEdit>(
                        metadata = ListDetailSceneStrategy.detailPane(
                            sceneKey = OVERVIEW_LIST_DETAIL_SCENE_KEY
                        )
                    ) { key ->
                        BikeEditScreen(
                            bikeId = key.bikeId,
                            viewModel = bikeEditViewModel,
                            preferencesRepository = preferencesRepository,
                            isDualPane = isScaffoldDualPane,
                            onNavigateBack = {
                                bikeEditViewModel.reset()
                                popDetail()
                            }
                        )
                    }
                    entry<Screen.Settings>(
                        metadata = ListDetailSceneStrategy.detailPane(
                            sceneKey = OVERVIEW_LIST_DETAIL_SCENE_KEY
                        )
                    ) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = {
                                popDetail()
                            },
                            onNavigateToOnboarding = {
                                backStack.clear()
                                backStack.add(Screen.Onboarding(1))
                            }
                        )
                    }
                    entry<Screen.AppInfo>(
                        metadata = ListDetailSceneStrategy.detailPane(
                            sceneKey = OVERVIEW_LIST_DETAIL_SCENE_KEY
                        )
                    ) {
                        AppInfoScreen(
                            isDualPane = isScaffoldDualPane,
                            onNavigateBack = {
                                popDetail()
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun OverviewDetailPlaceholder(
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tireColor = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
    val cardColors = appGlossCardColors(
        base = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        content = MaterialTheme.colorScheme.onSurface,
        isDark = isDark
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlossCard(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = cardColors,
                tiltOnTouch = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TirePressurePulseLogo(
                        size = 84.dp,
                        shape = RoundedCornerShape(20.dp),
                        contentDescription = stringResource(id = R.string.app_info_title)
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = tireColor)) {
                                append("Tire")
                            }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("Pressure")
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(id = R.string.onboarding_welcome_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.settings_title),
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenAppInfo,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.app_info_title),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
