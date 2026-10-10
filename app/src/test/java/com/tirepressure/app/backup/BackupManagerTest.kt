package com.tirepressure.app.backup

import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.data.backup.BackupManager
import com.tirepressure.app.data.backup.BackupResult
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BackupManagerTest {

    private lateinit var fakeBikeRepo: FakeBikeRepository
    private lateinit var fakePrefsRepo: FakePreferencesRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        fakeBikeRepo = FakeBikeRepository()
        fakePrefsRepo = FakePreferencesRepository()
        backupManager = BackupManager(fakeBikeRepo, fakePrefsRepo)
    }

    @Test
    fun testExportAndImportSuccess() = runBlocking {
        // Ajouter un vélo
        val originalBike = BikeProfile(
            name = "Mon Gravel Aventure",
            bikeWeightKg = 9.2
        )
        fakeBikeRepo.insertBike(originalBike)
        fakePrefsRepo.setPressureUnit(PressureUnit.PSI)

        // Exporter
        val json = backupManager.exportToJson()
        assertTrue("Le JSON doit contenir le nom du vélo", json.contains("Mon Gravel Aventure"))
        assertTrue("Le JSON doit contenir la clé schemaVersion", json.contains("\"schemaVersion\": 1"))

        // Valider & parser
        val parseResult = backupManager.validateAndParseJson(json)
        assertTrue("Le parsing doit réussir", parseResult is BackupResult.Success)

        val payload = (parseResult as BackupResult.Success).data
        assertEquals(1, payload.bikes.size)
        assertEquals("Mon Gravel Aventure", payload.bikes[0].name)
        assertEquals(PressureUnit.PSI, payload.preferences?.pressureUnit)

        // Restaurer
        val restoreResult = backupManager.restorePayload(payload, merge = false)
        assertTrue("La restauration doit réussir", restoreResult is BackupResult.Success)
    }

    @Test
    fun testRejectFutureSchemaVersion() {
        val futureJson = """
            {
                "schemaVersion": 99,
                "exportDate": "2030-01-01T00:00:00Z",
                "appVersion": "9.0.0",
                "bikes": []
            }
        """.trimIndent()

        val parseResult = backupManager.validateAndParseJson(futureJson)
        assertTrue("Doit être en erreur pour un schéma futur", parseResult is BackupResult.Error)
        val errorMsg = (parseResult as BackupResult.Error).message
        assertTrue(errorMsg.contains("version plus récente"))
    }

    @Test
    fun testRejectCorruptedJson() {
        val corruptedJson = "{ this is not valid json : [[{"

        val parseResult = backupManager.validateAndParseJson(corruptedJson)
        assertTrue("Doit rejeter un JSON malformé", parseResult is BackupResult.Error)
    }

    // Fakes pour les tests
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
        override val appLanguageFlow = MutableStateFlow(com.tirepressure.app.core.model.AppLanguage.SYSTEM)
        override val massUnitFlow = MutableStateFlow(MassUnit.KG)
        override val pressureUnitFlow = MutableStateFlow(PressureUnit.BAR)
        override val riderWeightFlow = MutableStateFlow(75.0)
        override val isOnboardingCompletedFlow = MutableStateFlow(true)
        override val selectedBikeIdFlow = MutableStateFlow<String?>(null)
        override val showWetSurfaceFlow = MutableStateFlow(true)
        override val showSnowSurfaceFlow = MutableStateFlow(true)

        override suspend fun setTheme(theme: AppTheme) { themeFlow.value = theme }
        override suspend fun setAppLanguage(language: com.tirepressure.app.core.model.AppLanguage) { appLanguageFlow.value = language }
        override suspend fun setMassUnit(unit: MassUnit) { massUnitFlow.value = unit }
        override suspend fun setPressureUnit(unit: PressureUnit) { pressureUnitFlow.value = unit }
        override suspend fun setRiderWeight(weightKg: Double) { riderWeightFlow.value = weightKg }
        override suspend fun setOnboardingCompleted(completed: Boolean) { isOnboardingCompletedFlow.value = completed }
        override suspend fun setSelectedBikeId(id: String?) { selectedBikeIdFlow.value = id }
        override suspend fun setShowWetSurface(show: Boolean) { showWetSurfaceFlow.value = show }
        override suspend fun setShowSnowSurface(show: Boolean) { showSnowSurfaceFlow.value = show }
        override suspend fun clear() {}
    }
}

