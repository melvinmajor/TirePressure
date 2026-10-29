package com.tirepressure.app.data.backup

import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(
    private val bikeRepository: BikeRepository,
    private val preferencesRepository: PreferencesRepository
) {
    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createBackupPayload(): BackupPayload {
        val bikes = bikeRepository.getAllBikes().first()
        val theme = preferencesRepository.themeFlow.first()
        val language = preferencesRepository.appLanguageFlow.first()
        val massUnit = preferencesRepository.massUnitFlow.first()
        val pressureUnit = preferencesRepository.pressureUnitFlow.first()
        val riderWeight = preferencesRepository.riderWeightFlow.first()
        val showWetSurface = preferencesRepository.showWetSurfaceFlow.first()
        val showSnowSurface = preferencesRepository.showSnowSurfaceFlow.first()
        val defaultOverviewCompact = preferencesRepository.defaultOverviewCompactFlow.first()
        val favoriteMode = preferencesRepository.favoriteModeFlow.first()
        val accentColor = preferencesRepository.accentColorFlow.first()
        val twoPaneSplitRatio = preferencesRepository.twoPaneSplitRatioFlow.first()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return BackupPayload(
            schemaVersion = 1,
            exportDate = dateFormat.format(Date()),
            appVersion = "1.0.0",
            bikes = bikes,
            preferences = AppPreferencesBackup(
                theme = theme,
                language = language,
                massUnit = massUnit,
                pressureUnit = pressureUnit,
                riderWeightKg = riderWeight,
                showWetSurface = showWetSurface,
                showSnowSurface = showSnowSurface,
                defaultOverviewCompact = defaultOverviewCompact,
                favoriteMode = favoriteMode,
                accentColor = accentColor,
                twoPaneSplitRatio = twoPaneSplitRatio
            )
        )
    }

    suspend fun exportToJson(): String {
        val payload = createBackupPayload()
        return jsonFormat.encodeToString(payload)
    }

    fun validateAndParseJson(jsonString: String): BackupResult<BackupPayload> {
        if (jsonString.isBlank()) {
            return BackupResult.Error("Le fichier de sauvegarde est vide.")
        }

        return try {
            val payload = jsonFormat.decodeFromString<BackupPayload>(jsonString)

            // 1. Vérification de la version du schéma
            if (payload.schemaVersion > 1) {
                return BackupResult.Error("Ce fichier a été créé avec une version plus récente de l'application (schéma v${payload.schemaVersion}). Veuillez mettre à jour l'application.")
            }
            if (payload.schemaVersion < 1) {
                return BackupResult.Error("Version de schéma de sauvegarde invalide (v${payload.schemaVersion}).")
            }

            // 2. Validation des vélos
            val seenIds = mutableSetOf<String>()
            for (bike in payload.bikes) {
                if (bike.name.isBlank()) {
                    return BackupResult.Error("Un des vélos dans le fichier n'a pas de nom valide.")
                }
                if (bike.bikeWeightKg <= 0.0 || bike.bikeWeightKg > 50.0) {
                    return BackupResult.Error("Poids de vélo aberrant détecté pour '${bike.name}': ${bike.bikeWeightKg} kg.")
                }
                for (setup in bike.setups) {
                    if (setup.frontTire.widthMm <= 0.0 || setup.rearTire.widthMm <= 0.0) {
                        return BackupResult.Error("Largeur de pneu invalide détectée pour '${bike.name}' (${setup.name}).")
                    }
                }
                if (!seenIds.add(bike.id)) {
                    return BackupResult.Error("Identifiant de vélo dupliqué détecté dans le fichier : ${bike.id}")
                }
            }

            BackupResult.Success(payload)
        } catch (e: Exception) {
            BackupResult.Error("Format de fichier JSON invalide ou corrompu : ${e.localizedMessage}", e)
        }
    }

    suspend fun restorePayload(payload: BackupPayload, merge: Boolean): BackupResult<Int> {
        return try {
            val currentBikes = bikeRepository.getAllBikes().first()

            val finalBikes = if (merge) {
                val currentMap = currentBikes.associateBy { it.id }.toMutableMap()
                for (imported in payload.bikes) {
                    if (currentMap.containsKey(imported.id)) {
                        // Pour éviter d'écraser un vélo existant lors de la fusion, générer un nouvel id
                        val nonColliding = imported.copy(
                            id = UUID.randomUUID().toString(),
                            name = "${imported.name} (importé)",
                            isDefault = false
                        )
                        currentMap[nonColliding.id] = nonColliding
                    } else {
                        currentMap[imported.id] = imported
                    }
                }
                currentMap.values.toList()
            } else {
                // Remplacement total
                payload.bikes
            }

            // Transaction sécurisée
            bikeRepository.replaceAll(finalBikes)

            // Restauration des préférences si présentes
            payload.preferences?.let { prefs ->
                preferencesRepository.setTheme(prefs.theme)
                preferencesRepository.setAppLanguage(prefs.language)
                preferencesRepository.setMassUnit(prefs.massUnit)
                preferencesRepository.setPressureUnit(prefs.pressureUnit)
                preferencesRepository.setRiderWeight(prefs.riderWeightKg)
                preferencesRepository.setShowWetSurface(prefs.showWetSurface)
                preferencesRepository.setShowSnowSurface(prefs.showSnowSurface)
                preferencesRepository.setDefaultOverviewCompact(prefs.defaultOverviewCompact)
                preferencesRepository.setFavoriteMode(prefs.favoriteMode)
                preferencesRepository.setAccentColor(prefs.accentColor)
                preferencesRepository.setTwoPaneSplitRatio(prefs.twoPaneSplitRatio)
            }

            BackupResult.Success(finalBikes.size)
        } catch (e: Exception) {
            BackupResult.Error("Échec lors de l'application de la restauration : ${e.localizedMessage}", e)
        }
    }
}

