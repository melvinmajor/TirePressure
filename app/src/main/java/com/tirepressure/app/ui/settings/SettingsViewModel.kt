package com.tirepressure.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.data.backup.BackupManager
import com.tirepressure.app.data.backup.BackupPayload
import com.tirepressure.app.data.backup.BackupResult
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val riderWeightKg: Double = 75.0,
    val massUnit: MassUnit = MassUnit.KG,
    val pressureUnit: PressureUnit = PressureUnit.BAR,
    val showWetSurface: Boolean = true,
    val showSnowSurface: Boolean = true,
    val showResetConfirmation: Boolean = false,
    val pendingImportPayload: BackupPayload? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
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
            combine(
                preferencesRepository.themeFlow,
                preferencesRepository.appLanguageFlow,
                preferencesRepository.riderWeightFlow,
                preferencesRepository.massUnitFlow,
                preferencesRepository.pressureUnitFlow,
                preferencesRepository.showWetSurfaceFlow,
                preferencesRepository.showSnowSurfaceFlow
            ) { flows ->
                @Suppress("UNCHECKED_CAST")
                val theme = flows[0] as AppTheme
                val language = flows[1] as AppLanguage
                val riderWeight = flows[2] as Double
                val massUnit = flows[3] as MassUnit
                val pressureUnit = flows[4] as PressureUnit
                val showWet = flows[5] as Boolean
                val showSnow = flows[6] as Boolean
                _uiState.update { current ->
                    current.copy(
                        theme = theme,
                        language = language,
                        riderWeightKg = riderWeight,
                        massUnit = massUnit,
                        pressureUnit = pressureUnit,
                        showWetSurface = showWet,
                        showSnowSurface = showSnow
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
                    statusMessage = "Toutes les données ont été réinitialisées."
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
                _uiState.update { it.copy(statusMessage = "Sauvegarde exportée avec succès !") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur lors de l'exportation : ${e.localizedMessage}") }
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
                        _uiState.update { it.copy(errorMessage = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Impossible de lire le fichier : ${e.localizedMessage}") }
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
                            statusMessage = "${result.data} vélo(s) restauré(s) avec succès !"
                        )
                    }
                }
                is BackupResult.Error -> {
                    _uiState.update {
                        it.copy(
                            pendingImportPayload = null,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}

