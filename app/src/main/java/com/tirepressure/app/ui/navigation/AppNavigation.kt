package com.tirepressure.app.ui.navigation

import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.window.core.layout.WindowSizeClass
import com.tirepressure.app.core.model.TwoPaneSplitRatio
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.bikes.BikeEditScreen
import com.tirepressure.app.ui.bikes.BikeEditViewModel
import com.tirepressure.app.ui.components.LocalGlassBackdrop
import com.tirepressure.app.ui.info.AppInfoScreen
import com.tirepressure.app.ui.onboarding.OnboardingScreen
import com.tirepressure.app.ui.overview.OverviewScreen
import com.tirepressure.app.ui.overview.OverviewViewModel
import com.tirepressure.app.ui.settings.SettingsScreen
import com.tirepressure.app.ui.settings.SettingsViewModel
import io.github.angelomarzocchi.flight.FlightHost
import io.github.angelomarzocchi.flight.FlightNavDisplay
import io.github.angelomarzocchi.glass.glassBackdrop
import io.github.angelomarzocchi.glass.rememberGlassBackdrop

/**
 * Computes the Material 3 Adaptive [PaneScaffoldDirective] tailored to the device form factor,
 * orientation, and physical [HingeInfo]:
 * - Portrait orientation (`isPortrait`), phones (Compact width < 600dp or Compact height < 480dp),
 *   folded outer displays, half-folded tabletop postures, and horizontal separating/occluding hinges
 *   always use a 1-partition layout (`maxHorizontalPartitions = 1`).
 * - Landscape unfolded foldables, tablets, and desktop windows (width >= 600dp AND height >= 480dp,
 *   without horizontal hinge occlusion or tabletop posture) use a 2-partition List-Detail layout
 *   (`maxHorizontalPartitions = 2`, clamping extra-large 3-partition defaults to 2 partitions).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun computeAdaptivePaneDirective(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    isPortrait: Boolean = false
): PaneScaffoldDirective {
    val isCompactHeight = !windowAdaptiveInfo.windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
    )
    val isTabletop = windowAdaptiveInfo.windowPosture.isTabletop
    val hinges: List<HingeInfo> = windowAdaptiveInfo.windowPosture.hingeList
    val hasHorizontalSeparatingHinge = hinges.any { !it.isVertical && (it.isSeparating || it.isOccluding) }

    return if (isPortrait || isCompactHeight || isTabletop || hasHorizontalSeparatingHinge) {
        calculatePaneScaffoldDirective(
            windowAdaptiveInfo = windowAdaptiveInfo,
            verticalHingePolicy = HingePolicy.AvoidOccluding
        ).copy(
            maxHorizontalPartitions = 1,
            maxVerticalPartitions = 1
        )
    } else {
        val baseDirective = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
            windowAdaptiveInfo = windowAdaptiveInfo,
            verticalHingePolicy = HingePolicy.AvoidOccluding
        )
        baseDirective.copy(
            maxHorizontalPartitions = baseDirective.maxHorizontalPartitions.coerceAtMost(2),
            maxVerticalPartitions = 1
        )
    }
}

/**
 * Resolves the active 2-partition split ratio using [HingeInfo] and device foldable capability:
 * - If the user explicitly selected a ratio in Settings (`isUserOverridden == true`), honor their choice.
 * - Otherwise, if running on a foldable device (detected via `windowPosture.hingeList` [HingeInfo] or
 *   hinge sensor capability), default to `50% – 50%` ([TwoPaneSplitRatio.HALF_HALF]).
 * - Otherwise, on tablets and desktops, default to `40% – 60%` ([TwoPaneSplitRatio.FORTY_SIXTY]).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun resolveEffectiveTwoPaneSplitRatio(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    isFoldableDevice: Boolean = false,
    userConfiguredRatio: TwoPaneSplitRatio = TwoPaneSplitRatio.FORTY_SIXTY,
    isUserOverridden: Boolean = false
): TwoPaneSplitRatio {
    if (isUserOverridden) return userConfiguredRatio
    val hasHingeInfo = windowAdaptiveInfo.windowPosture.hingeList.isNotEmpty()
    return if (hasHingeInfo || isFoldableDevice) {
        TwoPaneSplitRatio.HALF_HALF
    } else {
        TwoPaneSplitRatio.FORTY_SIXTY
    }
}

/**
 * Returns a stable, Bundle-compatible [String] key identifying the detail pane content.
 * Used both for [AnimatedContent] content keys and [androidx.compose.runtime.saveable.SaveableStateHolder]
 * keys, which only accept types storable in an Android Bundle.
 */
internal fun Screen?.detailStateKey(): String = when (this) {
    null -> "overview_detail_placeholder"
    is Screen.BikeEdit -> "bike_edit_${bikeId}_${initialSetupId ?: "default"}"
    is Screen.Settings -> "settings"
    is Screen.AppInfo -> "app_info"
    is Screen.Overview -> "overview"
    is Screen.Onboarding -> "onboarding_$step"
}

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
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val directive = remember(windowAdaptiveInfo, isPortrait) {
        computeAdaptivePaneDirective(
            windowAdaptiveInfo = windowAdaptiveInfo,
            isPortrait = isPortrait
        )
    }

    val overviewState by overviewViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val bikeEditState by bikeEditViewModel.uiState.collectAsStateWithLifecycle()

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
    val hasHingeSensor = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
    }
    var seenHingeInfo by rememberSaveable { mutableStateOf(false) }
    if (windowAdaptiveInfo.windowPosture.hingeList.isNotEmpty()) {
        seenHingeInfo = true
    }
    val isFoldableDevice = seenHingeInfo ||
        windowAdaptiveInfo.windowPosture.hingeList.isNotEmpty() ||
        hasHingeSensor
    val supportsTwoPaneLayout = isScaffoldDualPane ||
        configuration.smallestScreenWidthDp >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND ||
        windowAdaptiveInfo.windowPosture.isTabletop ||
        isFoldableDevice

    val effectiveTwoPaneSplitRatio = remember(
        windowAdaptiveInfo,
        isFoldableDevice,
        settingsState.twoPaneSplitRatio,
        settingsState.isTwoPaneSplitRatioUserOverridden
    ) {
        resolveEffectiveTwoPaneSplitRatio(
            windowAdaptiveInfo = windowAdaptiveInfo,
            isFoldableDevice = isFoldableDevice,
            userConfiguredRatio = settingsState.twoPaneSplitRatio,
            isUserOverridden = settingsState.isTwoPaneSplitRatioUserOverridden
        )
    }
    val animatedListProportion by animateFloatAsState(
        targetValue = effectiveTwoPaneSplitRatio.listWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "TwoPaneSplitProportion"
    )

    val isOnOnboarding = backStack.lastOrNull() is Screen.Onboarding

    fun navigateToDetail(target: Screen) {
        val currentTop = backStack.lastOrNull() as? Screen
        if (currentTop == target) return

        if (target is Screen.BikeEdit) {
            bikeEditViewModel.reset()
        }
        backStack.add(target)
    }

    fun popDetail() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    if (isOnOnboarding) {
        FlightHost(modifier = modifier.fillMaxSize()) {
            FlightNavDisplay(
                backStack = backStack,
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
                    entry<Screen.Overview> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                }
            )
        }
    } else {
        val currentDetailScreen = (backStack.lastOrNull() as? Screen)
            ?.takeIf { it !is Screen.Overview && it !is Screen.Onboarding }
        val isDualPaneActive = isScaffoldDualPane && currentDetailScreen != null

        BackHandler(enabled = currentDetailScreen != null) {
            popDetail()
        }

        val paneExpansionState = rememberPaneExpansionState()
        paneExpansionState.setFirstPaneProportion(animatedListProportion)

        val scaffoldValue = remember(isScaffoldDualPane, currentDetailScreen != null) {
            when {
                currentDetailScreen == null -> ThreePaneScaffoldValue(
                    primary = PaneAdaptedValue.Hidden,
                    secondary = PaneAdaptedValue.Expanded,
                    tertiary = PaneAdaptedValue.Hidden
                )
                isScaffoldDualPane -> ThreePaneScaffoldValue(
                    primary = PaneAdaptedValue.Expanded,
                    secondary = PaneAdaptedValue.Expanded,
                    tertiary = PaneAdaptedValue.Hidden
                )
                else -> ThreePaneScaffoldValue(
                    primary = PaneAdaptedValue.Expanded,
                    secondary = PaneAdaptedValue.Hidden,
                    tertiary = PaneAdaptedValue.Hidden
                )
            }
        }

        var isAppInfoPopupActive by remember { mutableStateOf(false) }
        val isAnyTwoPaneDialogActive = overviewState.bikeToDelete != null ||
            overviewState.setupToDelete != null ||
            overviewState.editingBikeDraft != null ||
            overviewState.addSetupDraft != null ||
            overviewState.showWhatsNewDialog ||
            isAppInfoPopupActive ||
            settingsState.showResetConfirmation ||
            settingsState.pendingImportPayload != null ||
            bikeEditState.showDeleteBikeConfirmation ||
            bikeEditState.setupToDeleteId != null ||
            bikeEditState.showAddSetupDialog
        val twoPaneBlurRadius by animateDpAsState(
            targetValue = if (isAnyTwoPaneDialogActive) 14.dp else 0.dp,
            animationSpec = tween(250),
            label = "TwoPaneDialogBlur"
        )
        val rootGlassBackdrop = rememberGlassBackdrop()

        CompositionLocalProvider(LocalGlassBackdrop provides rootGlassBackdrop) {
            ListDetailPaneScaffold(
                directive = directive,
                value = scaffoldValue,
                    listPane = {
                        AnimatedPane(
                            modifier = if (isDualPaneActive) {
                                Modifier
                                    .preferredWidth(animatedListProportion)
                                    .fillMaxHeight()
                            } else {
                                Modifier.fillMaxSize()
                            }
                        ) {
                            OverviewScreen(
                                viewModel = overviewViewModel,
                                isDualPane = isDualPaneActive,
                                onNavigateToAddBike = {
                                    navigateToDetail(Screen.BikeEdit("new"))
                                },
                                onNavigateToEditBike = { bikeId, setupId ->
                                    navigateToDetail(Screen.BikeEdit(bikeId, setupId))
                                },
                                onNavigateToSettings = {
                                    navigateToDetail(Screen.Settings)
                                },
                                onNavigateToAppInfo = {
                                    navigateToDetail(Screen.AppInfo)
                                }
                            )
                        }
                    },
                    detailPane = {
                        AnimatedPane(
                            modifier = if (isDualPaneActive) {
                                Modifier
                                    .preferredWidth(1f - animatedListProportion)
                                    .fillMaxHeight()
                            } else {
                                Modifier.fillMaxSize()
                            }
                        ) {
                            val detailSaveableStateHolder = rememberSaveableStateHolder()
                            AnimatedContent(
                                targetState = currentDetailScreen,
                                transitionSpec = {
                                    val isOpeningDetail = initialState == null && targetState != null
                                    val isClosingToPlaceholder = initialState != null && targetState == null
                                    when {
                                        isOpeningDetail -> {
                                            (slideInHorizontally(
                                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                                initialOffsetX = { fullWidth -> fullWidth / 6 }
                                            ) + fadeIn(
                                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                                            ) + scaleIn(
                                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                                initialScale = 0.96f
                                            )) togetherWith (fadeOut(
                                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                                            ) + scaleOut(
                                                animationSpec = tween(250, easing = FastOutSlowInEasing),
                                                targetScale = 0.96f
                                            ))
                                        }
                                        isClosingToPlaceholder -> {
                                            (fadeIn(
                                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                                            ) + scaleIn(
                                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                                initialScale = 0.96f
                                            )) togetherWith (slideOutHorizontally(
                                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                                targetOffsetX = { fullWidth -> fullWidth / 6 }
                                            ) + fadeOut(
                                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                                            ) + scaleOut(
                                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                                targetScale = 0.96f
                                            ))
                                        }
                                        else -> {
                                            (slideInHorizontally(
                                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                                initialOffsetX = { fullWidth -> fullWidth / 10 }
                                            ) + fadeIn(
                                                animationSpec = tween(240, easing = FastOutSlowInEasing)
                                            ) + scaleIn(
                                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                                initialScale = 0.98f
                                            )) togetherWith (slideOutHorizontally(
                                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                                                targetOffsetX = { fullWidth -> -fullWidth / 12 }
                                            ) + fadeOut(
                                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                                            ) + scaleOut(
                                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                                                targetScale = 0.98f
                                            ))
                                        }
                                    }.using(SizeTransform(clip = true))
                                },
                                contentKey = { it.detailStateKey() },
                                modifier = Modifier.fillMaxSize(),
                                label = "DetailPaneAnimatedContent"
                            ) { targetDetail ->
                                // SaveableStateProvider keys must be Bundle-compatible (String), never a Screen object,
                                // otherwise Compose throws "Type of the key ... is not supported" and the app crashes.
                                detailSaveableStateHolder.SaveableStateProvider(
                                    key = targetDetail.detailStateKey()
                                ) {
                                    when (targetDetail) {
                                        null -> Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.background)
                                        )
                                        is Screen.BikeEdit -> key(targetDetail.bikeId, targetDetail.initialSetupId) {
                                            BikeEditScreen(
                                                bikeId = targetDetail.bikeId,
                                                initialSetupId = targetDetail.initialSetupId,
                                                viewModel = bikeEditViewModel,
                                                preferencesRepository = preferencesRepository,
                                                isDualPane = isDualPaneActive,
                                                onNavigateBack = {
                                                    popDetail()
                                                }
                                            )
                                        }
                                        is Screen.Settings -> SettingsScreen(
                                            viewModel = settingsViewModel,
                                            supportsTwoPaneLayout = supportsTwoPaneLayout,
                                            effectiveTwoPaneSplitRatio = effectiveTwoPaneSplitRatio,
                                            onNavigateBack = {
                                                popDetail()
                                            },
                                            onNavigateToOnboarding = {
                                                backStack.clear()
                                                backStack.add(Screen.Onboarding(1))
                                            }
                                        )
                                        is Screen.AppInfo -> AppInfoScreen(
                                            isDualPane = isDualPaneActive,
                                            onPopupActiveChanged = { isAppInfoPopupActive = it },
                                            onNavigateBack = {
                                                popDetail()
                                            }
                                        )
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    },
                paneExpansionState = paneExpansionState,
                modifier = modifier
                    .fillMaxSize()
                    .glassBackdrop(rootGlassBackdrop)
                    .background(MaterialTheme.colorScheme.background)
                    .blur(twoPaneBlurRadius)
            )
        }
    }
}
