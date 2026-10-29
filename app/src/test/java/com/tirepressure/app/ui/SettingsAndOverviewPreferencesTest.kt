package com.tirepressure.app.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.FavoriteMode
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.model.TwoPaneSplitRatio
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.data.backup.BackupManager
import com.tirepressure.app.domain.model.BikeCategory
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.model.WheelSetup
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.navigation.computeAdaptivePaneDirective
import com.tirepressure.app.ui.overview.OverviewViewModel
import com.tirepressure.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit TestRule that configures [Dispatchers.Main] with an [UnconfinedTestDispatcher]
 * for deterministic ViewModel and coroutine preference testing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3AdaptiveApi::class)
class SettingsAndOverviewPreferencesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePrefsRepo: FakePreferencesRepository
    private lateinit var fakeBikeRepo: FakeBikeRepository
    private lateinit var backupManager: BackupManager
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var overviewViewModel: OverviewViewModel

    @Before
    fun setUp() {
        fakePrefsRepo = FakePreferencesRepository()
        fakeBikeRepo = FakeBikeRepository()
        backupManager = BackupManager(fakeBikeRepo, fakePrefsRepo)
        settingsViewModel = SettingsViewModel(
            preferencesRepository = fakePrefsRepo,
            bikeRepository = fakeBikeRepo,
            backupManager = backupManager
        )
        overviewViewModel = OverviewViewModel(
            bikeRepository = fakeBikeRepo,
            preferencesRepository = fakePrefsRepo
        )
    }

    @Test
    fun `app language change updates preferences and SettingsUiState`() = runTest {
        assertEquals(AppLanguage.SYSTEM, settingsViewModel.uiState.value.language)
        assertEquals("", AppLanguage.SYSTEM.code)

        settingsViewModel.setLanguage(AppLanguage.FRENCH)
        assertEquals(AppLanguage.FRENCH, settingsViewModel.uiState.value.language)
        assertEquals(AppLanguage.FRENCH, fakePrefsRepo.appLanguageFlow.value)
        assertEquals("fr", AppLanguage.FRENCH.code)

        settingsViewModel.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, settingsViewModel.uiState.value.language)
        assertEquals(AppLanguage.ENGLISH, fakePrefsRepo.appLanguageFlow.value)
        assertEquals("en", AppLanguage.ENGLISH.code)

        settingsViewModel.setLanguage(AppLanguage.SYSTEM)
        assertEquals(AppLanguage.SYSTEM, settingsViewModel.uiState.value.language)
    }

    @Test
    fun `pressure and weight unit changes synchronize across Settings and Overview`() = runTest {
        assertEquals(PressureUnit.BAR, settingsViewModel.uiState.value.pressureUnit)
        assertEquals(MassUnit.KG, settingsViewModel.uiState.value.massUnit)
        assertEquals(PressureUnit.BAR, overviewViewModel.uiState.value.pressureUnit)
        assertEquals(MassUnit.KG, overviewViewModel.uiState.value.massUnit)

        // Change pressure unit to PSI and KPA
        settingsViewModel.setPressureUnit(PressureUnit.PSI)
        assertEquals(PressureUnit.PSI, settingsViewModel.uiState.value.pressureUnit)
        assertEquals(PressureUnit.PSI, overviewViewModel.uiState.value.pressureUnit)
        assertEquals(29.0, UnitConverter.fromMbar(2000.0, PressureUnit.PSI), 0.1)

        settingsViewModel.setPressureUnit(PressureUnit.KPA)
        assertEquals(PressureUnit.KPA, settingsViewModel.uiState.value.pressureUnit)
        assertEquals(PressureUnit.KPA, overviewViewModel.uiState.value.pressureUnit)
        assertEquals(200.0, UnitConverter.fromMbar(2000.0, PressureUnit.KPA), 0.1)

        // Change weight unit to LB and back to KG
        settingsViewModel.setMassUnit(MassUnit.LB)
        assertEquals(MassUnit.LB, settingsViewModel.uiState.value.massUnit)
        assertEquals(MassUnit.LB, overviewViewModel.uiState.value.massUnit)
        val lbs = UnitConverter.fromKg(75.0, MassUnit.LB)
        assertEquals(75.0, UnitConverter.toKg(lbs, MassUnit.LB), 0.01)

        // Update rider weight and verify Overview recalculates pressures
        fakeBikeRepo.insertBike(
            BikeProfile(
                id = "road-1",
                name = "Aero Road",
                category = BikeCategory.ROAD,
                bikeWeightKg = 7.8,
                setups = listOf(WheelSetup(name = "Race"))
            )
        )
        val initialFrontBar = overviewViewModel.uiState.value.bikesWithPressures
            .first().setupsWithPressures.first().dryResult.front.pressureBar

        settingsViewModel.setRiderWeight(90.0)
        assertEquals(90.0, settingsViewModel.uiState.value.riderWeightKg, 0.001)
        assertEquals(90.0, overviewViewModel.uiState.value.riderWeightKg, 0.001)

        val heavierFrontBar = overviewViewModel.uiState.value.bikesWithPressures
            .first().setupsWithPressures.first().dryResult.front.pressureBar
        assertTrue(
            "Heavier rider weight must increase calculated tire pressure",
            heavierFrontBar > initialFrontBar
        )
    }

    @Test
    fun `selected calculated surface conditions change updates Settings and Overview`() = runTest {
        fakeBikeRepo.insertBike(
            BikeProfile(
                id = "gravel-1",
                name = "Gravel Explorer",
                category = BikeCategory.GRAVEL,
                bikeWeightKg = 9.0,
                setups = listOf(WheelSetup(name = "Mixed"))
            )
        )

        assertTrue(settingsViewModel.uiState.value.showWetSurface)
        assertTrue(settingsViewModel.uiState.value.showSnowSurface)
        assertTrue(overviewViewModel.uiState.value.showWetSurface)
        assertTrue(overviewViewModel.uiState.value.showSnowSurface)

        // Disable wet surface
        settingsViewModel.setShowWetSurface(false)
        assertFalse(settingsViewModel.uiState.value.showWetSurface)
        assertFalse(overviewViewModel.uiState.value.showWetSurface)

        // Disable snow surface
        settingsViewModel.setShowSnowSurface(false)
        assertFalse(settingsViewModel.uiState.value.showSnowSurface)
        assertFalse(overviewViewModel.uiState.value.showSnowSurface)

        // Re-enable both
        settingsViewModel.setShowWetSurface(true)
        settingsViewModel.setShowSnowSurface(true)
        assertTrue(overviewViewModel.uiState.value.showWetSurface)
        assertTrue(overviewViewModel.uiState.value.showSnowSurface)

        // Verify multi-surface pressure ordering: Dry > Wet > Snow
        val setupResult = overviewViewModel.uiState.value.bikesWithPressures.first().setupsWithPressures.first()
        assertTrue(setupResult.dryResult.front.pressureBar > setupResult.wetResult.front.pressureBar)
        assertTrue(setupResult.wetResult.front.pressureBar > setupResult.snowResult.front.pressureBar)
    }

    @Test
    fun `appearance and accent color changes update SettingsUiState`() = runTest {
        assertEquals(AppTheme.SYSTEM, settingsViewModel.uiState.value.theme)
        for (theme in AppTheme.entries) {
            settingsViewModel.setTheme(theme)
            assertEquals(theme, settingsViewModel.uiState.value.theme)
            assertEquals(theme, fakePrefsRepo.themeFlow.value)
        }

        assertEquals(AppAccentColor.ELECTRIC_LIME, settingsViewModel.uiState.value.accentColor)
        for (accent in AppAccentColor.entries) {
            settingsViewModel.setAccentColor(accent)
            assertEquals(accent, settingsViewModel.uiState.value.accentColor)
            assertEquals(accent, fakePrefsRepo.accentColorFlow.value)
            // Verify non-zero primary color ints for both dark and light modes
            assertTrue(accent.getPrimaryColorInt(isDark = true) != 0)
            assertTrue(accent.getPrimaryColorInt(isDark = false) != 0)
        }
    }

    @Test
    fun `default overview layout change synchronizes across Settings and Overview`() = runTest {
        assertFalse(settingsViewModel.uiState.value.defaultOverviewCompact)
        assertFalse(overviewViewModel.uiState.value.defaultOverviewCompact)

        settingsViewModel.setDefaultOverviewCompact(true)
        assertTrue(settingsViewModel.uiState.value.defaultOverviewCompact)
        assertTrue(overviewViewModel.uiState.value.defaultOverviewCompact)
        assertTrue(fakePrefsRepo.defaultOverviewCompactFlow.value)

        settingsViewModel.setDefaultOverviewCompact(false)
        assertFalse(settingsViewModel.uiState.value.defaultOverviewCompact)
        assertFalse(overviewViewModel.uiState.value.defaultOverviewCompact)
    }

    @Test
    fun `default 2-partition layout split ratio defaults to 40 - 60 and persists 1_2 - 1_2 change`() = runTest {
        // Default must be 40% - 60%
        assertEquals(
            TwoPaneSplitRatio.FORTY_SIXTY,
            settingsViewModel.uiState.value.twoPaneSplitRatio
        )
        assertEquals(0.4f, settingsViewModel.uiState.value.twoPaneSplitRatio.listWeight, 0.0001f)
        assertEquals(0.6f, settingsViewModel.uiState.value.twoPaneSplitRatio.detailWeight, 0.0001f)

        // Switch to 1/2 - 1/2
        settingsViewModel.setTwoPaneSplitRatio(TwoPaneSplitRatio.HALF_HALF)
        assertEquals(
            TwoPaneSplitRatio.HALF_HALF,
            settingsViewModel.uiState.value.twoPaneSplitRatio
        )
        assertEquals(TwoPaneSplitRatio.HALF_HALF, fakePrefsRepo.twoPaneSplitRatioFlow.value)
        assertEquals(0.5f, settingsViewModel.uiState.value.twoPaneSplitRatio.listWeight, 0.0001f)
        assertEquals(0.5f, settingsViewModel.uiState.value.twoPaneSplitRatio.detailWeight, 0.0001f)

        // Simulate app restart by creating a new SettingsViewModel backed by the same persisted repository
        val restartedViewModel = SettingsViewModel(
            preferencesRepository = fakePrefsRepo,
            bikeRepository = fakeBikeRepo,
            backupManager = backupManager
        )
        assertEquals(
            TwoPaneSplitRatio.HALF_HALF,
            restartedViewModel.uiState.value.twoPaneSplitRatio
        )

        // Switch back to 40% - 60%
        restartedViewModel.setTwoPaneSplitRatio(TwoPaneSplitRatio.FORTY_SIXTY)
        assertEquals(
            TwoPaneSplitRatio.FORTY_SIXTY,
            restartedViewModel.uiState.value.twoPaneSplitRatio
        )
    }

    @Test
    fun `computeAdaptivePaneDirective applies 1 or 2 partitions by device form factor`() {
        // 1. Phone in portrait (390x844dp -> Compact width, Expanded height -> 1 partition)
        val phonePortraitInfo = WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass.compute(390f, 844f),
            windowPosture = Posture()
        )
        val phonePortraitDirective = computeAdaptivePaneDirective(phonePortraitInfo)
        assertEquals(1, phonePortraitDirective.maxHorizontalPartitions)

        // 2. Phone in landscape (844x390dp -> Expanded width, Compact height < 480dp -> 1 partition)
        val phoneLandscapeInfo = WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass.compute(844f, 390f),
            windowPosture = Posture()
        )
        val phoneLandscapeDirective = computeAdaptivePaneDirective(phoneLandscapeInfo)
        assertEquals(1, phoneLandscapeDirective.maxHorizontalPartitions)

        // 3. Unfolded foldable (e.g. Galaxy Z Fold8 unfolded: 720x840dp -> Medium width, Medium height -> 2 partitions)
        val unfoldedFoldableInfo = WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass.compute(720f, 840f),
            windowPosture = Posture()
        )
        val unfoldedFoldableDirective = computeAdaptivePaneDirective(unfoldedFoldableInfo)
        assertEquals(2, unfoldedFoldableDirective.maxHorizontalPartitions)
        assertEquals(1, unfoldedFoldableDirective.maxVerticalPartitions)

        // 4. Tablet in landscape (1280x800dp -> Expanded width, Medium height -> 2 partitions)
        val tabletLandscapeInfo = WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass.compute(1280f, 800f),
            windowPosture = Posture()
        )
        val tabletLandscapeDirective = computeAdaptivePaneDirective(tabletLandscapeInfo)
        assertEquals(2, tabletLandscapeDirective.maxHorizontalPartitions)

        // 5. Extra-large desktop / tablet window (1700x1000dp -> clamped to 2 partitions)
        val extraLargeInfo = WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass.compute(1700f, 1000f),
            windowPosture = Posture()
        )
        val extraLargeDirective = computeAdaptivePaneDirective(extraLargeInfo)
        assertEquals(2, extraLargeDirective.maxHorizontalPartitions)

        // 6. Half-folded tabletop posture -> 1 horizontal partition
        val tabletopInfo = WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass.compute(720f, 840f),
            windowPosture = Posture(isTabletop = true)
        )
        val tabletopDirective = computeAdaptivePaneDirective(tabletopInfo)
        assertEquals(1, tabletopDirective.maxHorizontalPartitions)
    }

    @Test
    fun `confirmReset restores all settings and 2-partition split ratio to defaults`() = runTest {
        settingsViewModel.setLanguage(AppLanguage.FRENCH)
        settingsViewModel.setTheme(AppTheme.DARK)
        settingsViewModel.setAccentColor(AppAccentColor.PURPLE)
        settingsViewModel.setPressureUnit(PressureUnit.PSI)
        settingsViewModel.setMassUnit(MassUnit.LB)
        settingsViewModel.setShowWetSurface(false)
        settingsViewModel.setShowSnowSurface(false)
        settingsViewModel.setDefaultOverviewCompact(true)
        settingsViewModel.setTwoPaneSplitRatio(TwoPaneSplitRatio.HALF_HALF)

        var resetCallbackInvoked = false
        settingsViewModel.confirmReset { resetCallbackInvoked = true }

        assertTrue(resetCallbackInvoked)
        val state = settingsViewModel.uiState.value
        assertEquals(AppLanguage.SYSTEM, state.language)
        assertEquals(AppTheme.SYSTEM, state.theme)
        assertEquals(AppAccentColor.ELECTRIC_LIME, state.accentColor)
        assertEquals(PressureUnit.BAR, state.pressureUnit)
        assertEquals(MassUnit.KG, state.massUnit)
        assertTrue(state.showWetSurface)
        assertTrue(state.showSnowSurface)
        assertFalse(state.defaultOverviewCompact)
        assertEquals(TwoPaneSplitRatio.FORTY_SIXTY, state.twoPaneSplitRatio)
    }

    private class FakeBikeRepository : BikeRepository {
        private val bikes = mutableListOf<BikeProfile>()
        private val flow = MutableStateFlow<List<BikeProfile>>(emptyList())

        override fun getAllBikes(): Flow<List<BikeProfile>> = flow
        override suspend fun getAllBikesList(): List<BikeProfile> = bikes.toList()
        override suspend fun getBikeById(id: String): BikeProfile? = bikes.find { it.id == id }
        override suspend fun insertBike(bike: BikeProfile) {
            bikes.add(bike)
            flow.value = bikes.toList()
        }
        override suspend fun updateBike(bike: BikeProfile) {
            bikes.removeAll { it.id == bike.id }
            bikes.add(bike)
            flow.value = bikes.toList()
        }
        override suspend fun deleteBike(id: String) {
            bikes.removeAll { it.id == id }
            flow.value = bikes.toList()
        }
        override suspend fun setDefaultBike(id: String) {}
        override suspend fun toggleFavoriteBike(id: String) {}
        override suspend fun reconcileFavorites(mode: FavoriteMode) {}
        override suspend fun duplicateBike(id: String): BikeProfile? = null
        override suspend fun replaceAll(newBikes: List<BikeProfile>) {
            bikes.clear()
            bikes.addAll(newBikes)
            flow.value = bikes.toList()
        }
        override suspend fun clearAll() {
            bikes.clear()
            flow.value = emptyList()
        }
    }

    private class FakePreferencesRepository : PreferencesRepository {
        override val themeFlow = MutableStateFlow(AppTheme.SYSTEM)
        override val appLanguageFlow = MutableStateFlow(AppLanguage.SYSTEM)
        override val massUnitFlow = MutableStateFlow(MassUnit.KG)
        override val pressureUnitFlow = MutableStateFlow(PressureUnit.BAR)
        override val riderWeightFlow = MutableStateFlow(75.0)
        override val isOnboardingCompletedFlow = MutableStateFlow(true)
        override val selectedBikeIdFlow = MutableStateFlow<String?>(null)
        override val showWetSurfaceFlow = MutableStateFlow(true)
        override val showSnowSurfaceFlow = MutableStateFlow(true)
        override val defaultOverviewCompactFlow = MutableStateFlow(false)
        override val favoriteModeFlow = MutableStateFlow(FavoriteMode.GLOBAL)
        override val accentColorFlow = MutableStateFlow(AppAccentColor.ELECTRIC_LIME)
        override val twoPaneSplitRatioFlow = MutableStateFlow(TwoPaneSplitRatio.FORTY_SIXTY)

        override suspend fun setTheme(theme: AppTheme) { themeFlow.value = theme }
        override suspend fun setAppLanguage(language: AppLanguage) { appLanguageFlow.value = language }
        override suspend fun setMassUnit(unit: MassUnit) { massUnitFlow.value = unit }
        override suspend fun setPressureUnit(unit: PressureUnit) { pressureUnitFlow.value = unit }
        override suspend fun setRiderWeight(weightKg: Double) { riderWeightFlow.value = weightKg }
        override suspend fun setOnboardingCompleted(completed: Boolean) { isOnboardingCompletedFlow.value = completed }
        override suspend fun setSelectedBikeId(id: String?) { selectedBikeIdFlow.value = id }
        override suspend fun setShowWetSurface(show: Boolean) { showWetSurfaceFlow.value = show }
        override suspend fun setShowSnowSurface(show: Boolean) { showSnowSurfaceFlow.value = show }
        override suspend fun setDefaultOverviewCompact(compact: Boolean) { defaultOverviewCompactFlow.value = compact }
        override suspend fun setFavoriteMode(mode: FavoriteMode) { favoriteModeFlow.value = mode }
        override suspend fun setAccentColor(color: AppAccentColor) { accentColorFlow.value = color }
        override suspend fun setTwoPaneSplitRatio(ratio: TwoPaneSplitRatio) { twoPaneSplitRatioFlow.value = ratio }
        override suspend fun clear() {
            themeFlow.value = AppTheme.SYSTEM
            appLanguageFlow.value = AppLanguage.SYSTEM
            massUnitFlow.value = MassUnit.KG
            pressureUnitFlow.value = PressureUnit.BAR
            riderWeightFlow.value = 75.0
            isOnboardingCompletedFlow.value = false
            selectedBikeIdFlow.value = null
            showWetSurfaceFlow.value = true
            showSnowSurfaceFlow.value = true
            defaultOverviewCompactFlow.value = false
            favoriteModeFlow.value = FavoriteMode.GLOBAL
            accentColorFlow.value = AppAccentColor.ELECTRIC_LIME
            twoPaneSplitRatioFlow.value = TwoPaneSplitRatio.FORTY_SIXTY
        }
    }
}

