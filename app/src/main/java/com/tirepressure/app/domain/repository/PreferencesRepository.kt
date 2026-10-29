package com.tirepressure.app.domain.repository

import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.FavoriteMode
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.model.TwoPaneSplitRatio
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val themeFlow: Flow<AppTheme>
    val appLanguageFlow: Flow<AppLanguage>
    val massUnitFlow: Flow<MassUnit>
    val pressureUnitFlow: Flow<PressureUnit>
    val riderWeightFlow: Flow<Double>
    val isOnboardingCompletedFlow: Flow<Boolean>
    val selectedBikeIdFlow: Flow<String?>
    val showWetSurfaceFlow: Flow<Boolean>
    val showSnowSurfaceFlow: Flow<Boolean>
    val defaultOverviewCompactFlow: Flow<Boolean>
    val favoriteModeFlow: Flow<FavoriteMode>
    val accentColorFlow: Flow<AppAccentColor>
    val twoPaneSplitRatioFlow: Flow<TwoPaneSplitRatio>

    suspend fun setTheme(theme: AppTheme)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setMassUnit(unit: MassUnit)
    suspend fun setPressureUnit(unit: PressureUnit)
    suspend fun setRiderWeight(weightKg: Double)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setSelectedBikeId(id: String?)
    suspend fun setShowWetSurface(show: Boolean)
    suspend fun setShowSnowSurface(show: Boolean)
    suspend fun setDefaultOverviewCompact(compact: Boolean)
    suspend fun setFavoriteMode(mode: FavoriteMode)
    suspend fun setAccentColor(color: AppAccentColor)
    suspend fun setTwoPaneSplitRatio(ratio: TwoPaneSplitRatio)
    suspend fun clear()
}
