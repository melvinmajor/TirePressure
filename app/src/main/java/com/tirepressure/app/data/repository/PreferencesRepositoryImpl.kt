package com.tirepressure.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tire_pressure_prefs")

class PreferencesRepositoryImpl(
    private val context: Context
) : PreferencesRepository {

    companion object {
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_MASS_UNIT = stringPreferencesKey("mass_unit")
        val KEY_PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        val KEY_RIDER_WEIGHT = doublePreferencesKey("user_rider_weight")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_SELECTED_BIKE_ID = stringPreferencesKey("selected_bike_id")
        val KEY_SHOW_WET_SURFACE = booleanPreferencesKey("show_wet_surface")
        val KEY_SHOW_SNOW_SURFACE = booleanPreferencesKey("show_snow_surface")
        val KEY_DEFAULT_OVERVIEW_COMPACT = booleanPreferencesKey("default_overview_compact")
        val KEY_FAVORITE_MODE = stringPreferencesKey("favorite_mode")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
    }

    override val favoriteModeFlow: Flow<com.tirepressure.app.core.model.FavoriteMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_FAVORITE_MODE] ?: com.tirepressure.app.core.model.FavoriteMode.GLOBAL.name
        runCatching { com.tirepressure.app.core.model.FavoriteMode.valueOf(raw) }.getOrDefault(com.tirepressure.app.core.model.FavoriteMode.GLOBAL)
    }

    override val accentColorFlow: Flow<com.tirepressure.app.core.model.AppAccentColor> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_ACCENT_COLOR] ?: com.tirepressure.app.core.model.AppAccentColor.ELECTRIC_LIME.name
        runCatching { com.tirepressure.app.core.model.AppAccentColor.valueOf(raw) }.getOrDefault(com.tirepressure.app.core.model.AppAccentColor.ELECTRIC_LIME)
    }

    override val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: AppTheme.SYSTEM.name
        runCatching { AppTheme.valueOf(raw) }.getOrDefault(AppTheme.SYSTEM)
    }

    override val appLanguageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_LANGUAGE] ?: AppLanguage.SYSTEM.name
        runCatching { AppLanguage.valueOf(raw) }.getOrDefault(AppLanguage.SYSTEM)
    }

    override val massUnitFlow: Flow<MassUnit> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_MASS_UNIT] ?: MassUnit.KG.name
        runCatching { MassUnit.valueOf(raw) }.getOrDefault(MassUnit.KG)
    }

    override val pressureUnitFlow: Flow<PressureUnit> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_PRESSURE_UNIT] ?: PressureUnit.BAR.name
        runCatching { PressureUnit.valueOf(raw) }.getOrDefault(PressureUnit.BAR)
    }

    override val riderWeightFlow: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_RIDER_WEIGHT] ?: 75.0
    }

    override val isOnboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    override val selectedBikeIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_BIKE_ID]
    }

    override val showWetSurfaceFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_WET_SURFACE] ?: true
    }

    override val showSnowSurfaceFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_SNOW_SURFACE] ?: true
    }

    override val defaultOverviewCompactFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_OVERVIEW_COMPACT] ?: false
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.name
        }
        com.tirepressure.app.core.util.LocaleHelper.saveLanguage(context, language)
        com.tirepressure.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }

    override suspend fun setMassUnit(unit: MassUnit) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MASS_UNIT] = unit.name
        }
    }

    override suspend fun setPressureUnit(unit: PressureUnit) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRESSURE_UNIT] = unit.name
        }
        com.tirepressure.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }

    override suspend fun setRiderWeight(weightKg: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RIDER_WEIGHT] = weightKg
        }
        com.tirepressure.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = completed
        }
    }

    override suspend fun setSelectedBikeId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) {
                prefs[KEY_SELECTED_BIKE_ID] = id
            } else {
                prefs.remove(KEY_SELECTED_BIKE_ID)
            }
        }
    }

    override suspend fun setShowWetSurface(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_WET_SURFACE] = show
        }
        com.tirepressure.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }

    override suspend fun setShowSnowSurface(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_SNOW_SURFACE] = show
        }
        com.tirepressure.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }

    override suspend fun setDefaultOverviewCompact(compact: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_OVERVIEW_COMPACT] = compact
        }
    }

    override suspend fun setFavoriteMode(mode: com.tirepressure.app.core.model.FavoriteMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FAVORITE_MODE] = mode.name
        }
    }

    override suspend fun setAccentColor(color: com.tirepressure.app.core.model.AppAccentColor) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR] = color.name
        }
    }

    override suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
        com.tirepressure.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }
}
