package com.tirepressure.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.FavoriteMode
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.data.backup.BackupManager
import com.tirepressure.app.data.backup.BackupPayload
import com.tirepressure.app.data.backup.BackupResult
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.tirepressure.app.R
import java.io.InputStream
import java.io.OutputStream

data class UiText(
    val resId: Int? = null,
    val formatArgs: List<Any> = emptyList(),
    val rawText: String? = null
) {
    fun asString(context: android.content.Context): String {
        return if (resId != null) {
            context.getString(resId, *formatArgs.toTypedArray())
        } else {
            rawText.orEmpty()
        }
    }
}

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val favoriteMode: FavoriteMode = FavoriteMode.GLOBAL,
    val accentColor: AppAccentColor = AppAccentColor.ELECTRIC_LIME,
    val riderWeightKg: Double = 75.0,
    val massUnit: MassUnit = MassUnit.KG,
    val pressureUnit: PressureUnit = PressureUnit.BAR,
    val showWetSurface: Boolean = true,
    val showSnowSurface: Boolean = true,
    val defaultOverviewCompact: Boolean = false,
    val isUnitsExpanded: Boolean = true,
    val isDisplayExpanded: Boolean = false,
    val isWidgetsExpanded: Boolean = false,
    val isBackupExpanded: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val pendingImportPayload: BackupPayload? = null,
    val statusMessage: UiText? = null,
    val errorMessage: UiText? = null
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val bikeRepository: BikeRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val flowA = combine(
                preferencesRepository.themeFlow,
                preferencesRepository.appLanguageFlow,
                preferencesRepository.riderWeightFlow,
                preferencesRepository.massUnitFlow,
                preferencesRepository.pressureUnitFlow
            ) { theme, language, riderWeight, massUnit, pressureUnit ->
                SettingsBase(theme, language, riderWeight, massUnit, pressureUnit)
            }

            val flowB = combine(
                preferencesRepository.showWetSurfaceFlow,
                preferencesRepository.showSnowSurfaceFlow,
                preferencesRepository.defaultOverviewCompactFlow,
                preferencesRepository.favoriteModeFlow,
                preferencesRepository.accentColorFlow
            ) { wet, snow, defaultCompact, favMode, accent ->
                SettingsExtras(wet, snow, defaultCompact, favMode, accent)
            }

            combine(flowA, flowB) { base, extras ->
                _uiState.update { current ->
                    current.copy(
                        theme = base.theme,
                        language = base.language,
                        riderWeightKg = base.riderWeight,
                        massUnit = base.massUnit,
                        pressureUnit = base.pressureUnit,
                        showWetSurface = extras.showWet,
                        showSnowSurface = extras.showSnow,
                        defaultOverviewCompact = extras.defaultCompact,
                        favoriteMode = extras.favMode,
                        accentColor = extras.accent
                    )
                }
            }.collect()
        }
    }

    fun setShowWetSurface(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowWetSurface(show)
        }
    }

    fun setShowSnowSurface(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowSnowSurface(show)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setAppLanguage(language)
        }
    }

    fun setFavoriteMode(mode: FavoriteMode) {
        viewModelScope.launch {
            preferencesRepository.setFavoriteMode(mode)
            bikeRepository.reconcileFavorites(mode)
        }
    }

    fun setAccentColor(color: AppAccentColor) {
        viewModelScope.launch {
            preferencesRepository.setAccentColor(color)
        }
    }

    fun toggleUnitsExpanded() {
        _uiState.update { it.copy(isUnitsExpanded = !it.isUnitsExpanded) }
    }

    fun toggleDisplayExpanded() {
        _uiState.update { it.copy(isDisplayExpanded = !it.isDisplayExpanded) }
    }

    fun toggleWidgetsExpanded() {
        _uiState.update { it.copy(isWidgetsExpanded = !it.isWidgetsExpanded) }
    }

    fun toggleBackupExpanded() {
        _uiState.update { it.copy(isBackupExpanded = !it.isBackupExpanded) }
    }

    fun setRiderWeight(weightKg: Double) {
        viewModelScope.launch {
            preferencesRepository.setRiderWeight(weightKg)
        }
    }

    fun setMassUnit(unit: MassUnit) {
        viewModelScope.launch {
            preferencesRepository.setMassUnit(unit)
        }
    }

    fun setPressureUnit(unit: PressureUnit) {
        viewModelScope.launch {
            preferencesRepository.setPressureUnit(unit)
        }
    }

    fun setDefaultOverviewCompact(compact: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDefaultOverviewCompact(compact)
        }
    }

    fun requestReset() {
        _uiState.update { it.copy(showResetConfirmation = true) }
    }

    fun cancelReset() {
        _uiState.update { it.copy(showResetConfirmation = false) }
    }

    fun confirmReset() {
        viewModelScope.launch {
            bikeRepository.clearAll()
            preferencesRepository.clear()
            _uiState.update {
                it.copy(
                    showResetConfirmation = false,
                    statusMessage = UiText(resId = R.string.settings_reset_success)
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun exportBackup(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val jsonString = backupManager.exportToJson()
                outputStream.bufferedWriter().use { it.write(jsonString) }
                _uiState.update { it.copy(statusMessage = UiText(resId = R.string.settings_backup_export_success)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = UiText(resId = R.string.settings_backup_export_error, formatArgs = listOf(e.localizedMessage ?: ""))) }
            }
        }
    }

    fun readAndValidateImport(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                when (val result = backupManager.validateAndParseJson(jsonString)) {
                    is BackupResult.Success -> {
                        _uiState.update { it.copy(pendingImportPayload = result.data) }
                    }
                    is BackupResult.Error -> {
                        _uiState.update { it.copy(errorMessage = UiText(rawText = result.message)) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = UiText(resId = R.string.settings_backup_import_read_error, formatArgs = listOf(e.localizedMessage ?: ""))) }
            }
        }
    }

    fun cancelImport() {
        _uiState.update { it.copy(pendingImportPayload = null) }
    }

    fun confirmImport(merge: Boolean) {
        val payload = _uiState.value.pendingImportPayload ?: return
        viewModelScope.launch {
            when (val result = backupManager.restorePayload(payload, merge)) {
                is BackupResult.Success -> {
                    _uiState.update {
                        it.copy(
                            pendingImportPayload = null,
                            statusMessage = UiText(resId = R.string.settings_backup_import_success, formatArgs = listOf(result.data))
                        )
                    }
                }
                is BackupResult.Error -> {
                    _uiState.update {
                        it.copy(
                            pendingImportPayload = null,
                            errorMessage = UiText(rawText = result.message)
                        )
                    }
                }
            }
        }
    }
}

private data class SettingsBase(
    val theme: AppTheme,
    val language: AppLanguage,
    val riderWeight: Double,
    val massUnit: MassUnit,
    val pressureUnit: PressureUnit
)

private data class SettingsExtras(
    val showWet: Boolean,
    val showSnow: Boolean,
    val defaultCompact: Boolean,
    val favMode: FavoriteMode,
    val accent: AppAccentColor
)

