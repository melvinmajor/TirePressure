package com.tirepressure.app.data.backup

import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.FavoriteMode
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.model.TwoPaneSplitRatio
import com.tirepressure.app.domain.model.BikeProfile
import kotlinx.serialization.Serializable

@Serializable
data class AppPreferencesBackup(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val massUnit: MassUnit = MassUnit.KG,
    val pressureUnit: PressureUnit = PressureUnit.BAR,
    val riderWeightKg: Double = 75.0,
    val showWetSurface: Boolean = true,
    val showSnowSurface: Boolean = true,
    val defaultOverviewCompact: Boolean = false,
    val favoriteMode: FavoriteMode = FavoriteMode.GLOBAL,
    val accentColor: AppAccentColor = AppAccentColor.ELECTRIC_LIME,
    val twoPaneSplitRatio: TwoPaneSplitRatio = TwoPaneSplitRatio.FORTY_SIXTY
)

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportDate: String,
    val appVersion: String = "1.0.0",
    val bikes: List<BikeProfile>,
    val preferences: AppPreferencesBackup? = null
)

sealed class BackupResult<out T> {
    data class Success<out T>(val data: T) : BackupResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : BackupResult<Nothing>()
}
