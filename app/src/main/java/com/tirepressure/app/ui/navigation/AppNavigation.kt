package com.tirepressure.app.ui.navigation

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
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

/**
 * Computes the Material 3 Adaptive [PaneScaffoldDirective] tailored to the device form factor:
 * - Phones in portrait or landscape (Compact width < 600dp or Compact height < 480dp) and folded
 *   foldables on the outer cover display use a 1-partition layout (`maxHorizontalPartitions = 1`).
 * - Unfolded foldables, tablets, and large screens (width >= 600dp AND height >= 480dp, not in
 *   half-folded tabletop posture) use a 2-partition List-Detail layout (`maxHorizontalPartitions = 2`),
 *   clamping extra-large 3-partition defaults to 2 partitions and using [HingePolicy.AvoidOccluding]
 *   so non-occluding flexible foldable creases support both 40% - 60% and 1/2 - 1/2 split ratios.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun computeAdaptivePaneDirective(windowAdaptiveInfo: WindowAdaptiveInfo): PaneScaffoldDirective {
    val isCompactHeight = !windowAdaptiveInfo.windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
    )
    val isTabletop = windowAdaptiveInfo.windowPosture.isTabletop
    return if (isCompactHeight || isTabletop) {
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
 * Returns a stable, Bundle-compatible [String] key identifying the detail pane content.
 * Used both for [AnimatedContent] content keys and [androidx.compose.runtime.saveable.SaveableStateHolder]
 * keys, which only accept types storable in an Android Bundle.
 */
internal fun Screen?.detailStateKey(): String = when (this) {
    null -> "overview_detail_placeholder"
    is Screen.BikeEdit -> "bike_edit_$bikeId"
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
    val directive = remember(windowAdaptiveInfo) {
        computeAdaptivePaneDirective(windowAdaptiveInfo)
    }

    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val animatedListProportion by animateFloatAsState(
        targetValue = settingsState.twoPaneSplitRatio.listWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "TwoPaneSplitProportion"
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
    val hasHingeSensor = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
    }
    val supportsTwoPaneLayout = isScaffoldDualPane ||
        configuration.smallestScreenWidthDp >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND ||
        windowAdaptiveInfo.windowPosture.isTabletop ||
        hasHingeSensor

    val isOnOnboarding = backStack.lastOrNull() is Screen.Onboarding
    val useTwoPaneLayout = isScaffoldDualPane && !isOnOnboarding

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

    if (useTwoPaneLayout) {
        val currentDetailScreen = (backStack.lastOrNull() as? Screen)
            ?.takeIf { it !is Screen.Overview && it !is Screen.Onboarding }

        BackHandler(enabled = currentDetailScreen != null) {
            popDetail()
        }

        val paneExpansionState = rememberPaneExpansionState()
        paneExpansionState.setFirstPaneProportion(animatedListProportion)

        val scaffoldValue = remember {
            ThreePaneScaffoldValue(
                primary = PaneAdaptedValue.Expanded,
                secondary = PaneAdaptedValue.Expanded,
                tertiary = PaneAdaptedValue.Hidden
            )
        }

        FlightHost(modifier = modifier.fillMaxSize()) {
            ListDetailPaneScaffold(
                directive = directive,
                value = scaffoldValue,
                listPane = {
                    AnimatedPane(
                        modifier = Modifier
                            .preferredWidth(animatedListProportion)
                            .fillMaxHeight()
                    ) {
                        OverviewScreen(
                            viewModel = overviewViewModel,
                            isDualPane = true,
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
                },
                detailPane = {
                    AnimatedPane(
                        modifier = Modifier
                            .preferredWidth(1f - animatedListProportion)
                            .fillMaxHeight()
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
                                    null -> OverviewDetailPlaceholder(
                                        onOpenSettings = { navigateToDetail(Screen.Settings) },
                                        onOpenAppInfo = { navigateToDetail(Screen.AppInfo) }
                                    )
                                    is Screen.BikeEdit -> key(targetDetail.bikeId) {
                                        BikeEditScreen(
                                            bikeId = targetDetail.bikeId,
                                            viewModel = bikeEditViewModel,
                                            preferencesRepository = preferencesRepository,
                                            isDualPane = true,
                                            onNavigateBack = {
                                                popDetail()
                                            }
                                        )
                                    }
                                    is Screen.Settings -> SettingsScreen(
                                        viewModel = settingsViewModel,
                                        supportsTwoPaneLayout = supportsTwoPaneLayout,
                                        onNavigateBack = {
                                            popDetail()
                                        },
                                        onNavigateToOnboarding = {
                                            backStack.clear()
                                            backStack.add(Screen.Onboarding(1))
                                        }
                                    )
                                    is Screen.AppInfo -> AppInfoScreen(
                                        isDualPane = true,
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
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
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
                        OverviewScreen(
                            viewModel = overviewViewModel,
                            isDualPane = false,
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
                    entry<Screen.BikeEdit> { key ->
                        BikeEditScreen(
                            bikeId = key.bikeId,
                            viewModel = bikeEditViewModel,
                            preferencesRepository = preferencesRepository,
                            isDualPane = false,
                            onNavigateBack = {
                                popDetail()
                            }
                        )
                    }
                    entry<Screen.Settings> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            supportsTwoPaneLayout = supportsTwoPaneLayout,
                            onNavigateBack = {
                                popDetail()
                            },
                            onNavigateToOnboarding = {
                                backStack.clear()
                                backStack.add(Screen.Onboarding(1))
                            }
                        )
                    }
                    entry<Screen.AppInfo> {
                        AppInfoScreen(
                            isDualPane = false,
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
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.settings_title),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenAppInfo,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.app_info_title),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
