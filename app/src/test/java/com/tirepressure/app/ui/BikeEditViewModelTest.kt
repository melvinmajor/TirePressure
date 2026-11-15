package com.tirepressure.app.ui

import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.domain.model.BikeCategory
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.model.RideStyle
import com.tirepressure.app.domain.model.TireConfig
import com.tirepressure.app.domain.model.WheelSetup
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.ui.bikes.BikeEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BikeEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeBikeRepo: FakeBikeRepository
    private lateinit var viewModel: BikeEditViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeBikeRepo = FakeBikeRepository()
        viewModel = BikeEditViewModel(fakeBikeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testConfigurationChangePreservesData() = runTest {
        // Initial load for new bike
        viewModel.loadBikeIfNeeded("new", MassUnit.KG, "Default Setup")
        testDispatcher.scheduler.advanceUntilIdle()

        // User enters some data
        viewModel.setName("Trek Emonda")
        viewModel.setBrand("Trek")
        viewModel.setModel("SLR 7")
        viewModel.setBikeCategory(BikeCategory.ROAD)

        assertEquals("Trek Emonda", viewModel.uiState.value.name)
        assertEquals("Trek", viewModel.uiState.value.brand)

        // Simulate configuration change (device rotation / fold) where loadBikeIfNeeded is called again
        viewModel.loadBikeIfNeeded("new", MassUnit.KG, "Default Setup")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify data was NOT lost / NOT reset
        assertEquals("Trek Emonda", viewModel.uiState.value.name)
        assertEquals("Trek", viewModel.uiState.value.brand)
        assertEquals("SLR 7", viewModel.uiState.value.model)
        assertEquals(BikeCategory.ROAD, viewModel.uiState.value.bikeCategory)
    }

    @Test
    fun testSaveBikePersistsToRepository() = runTest {
        viewModel.loadBikeIfNeeded("new", MassUnit.KG, "Default Setup")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setName("Specialized Tarmac")
        viewModel.setBrand("Specialized")

        var saved = false
        viewModel.saveBike(
            massUnit = MassUnit.KG,
            nameRequiredError = "Name required",
            onSaved = { saved = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onSaved callback should be called", saved)
        val bikes = fakeBikeRepo.getAllBikesList()
        assertEquals(1, bikes.size)
        assertEquals("Specialized Tarmac", bikes[0].name)
        assertEquals("Specialized", bikes[0].brand)
    }

    @Test
    fun testAddSetupDialogDraftPreservation() = runTest {
        viewModel.loadBikeIfNeeded("new", MassUnit.KG, "Default Setup")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startAddSetup("Configuration")
        assertTrue(viewModel.uiState.value.showAddSetupDialog)
        assertNotNull(viewModel.uiState.value.addSetupDraft)

        val draft = viewModel.uiState.value.addSetupDraft!!
        viewModel.updateAddSetupDraft(draft.copy(setupName = "Montagne"))
        assertEquals("Montagne", viewModel.uiState.value.addSetupDraft?.setupName)

        // Cancel dialog
        viewModel.cancelAddSetup()
        assertFalse(viewModel.uiState.value.showAddSetupDialog)
        assertNull(viewModel.uiState.value.addSetupDraft)
    }

    @Test
    fun testLoadBikeSelectsInitialSetupAndUpdatesOnNewSetupId() = runTest {
        val bike = BikeProfile(
            id = "bike-1",
            name = "Gravel Bike",
            category = BikeCategory.GRAVEL,
            bikeWeightKg = 9.5,
            setups = listOf(
                WheelSetup(id = "setup-1", name = "Road Wheelset", isDefault = true),
                WheelSetup(id = "setup-2", name = "Gravel 40mm"),
                WheelSetup(id = "setup-3", name = "Winter Mud")
            )
        )
        fakeBikeRepo.insertBike(bike)

        viewModel.loadBikeIfNeeded(bike.id, MassUnit.KG, "Default Setup", initialSetupId = "setup-2")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedSetupIndex)

        // Switching to a different setup of the same loaded bike updates selectedSetupIndex
        viewModel.loadBikeIfNeeded(bike.id, MassUnit.KG, "Default Setup", initialSetupId = "setup-3")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.selectedSetupIndex)
    }

    @Test
    fun testTireWidthUnitSavedAndFormattedWithMaxTwoDecimals() = runTest {
        assertEquals(
            "2.35 in",
            TireConfig(widthMm = 2.35 * 25.4, isWidthInInches = true).formatWidthWithUnit(RideStyle.ROAD, " in", "mm")
        )
        assertEquals(
            "59.69mm",
            TireConfig(widthMm = 2.35 * 25.4, isWidthInInches = false).formatWidthWithUnit(RideStyle.XC_MTB, " in", "mm")
        )

        viewModel.loadBikeIfNeeded("new", MassUnit.KG, "Default Setup")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setName("Trail MTB")
        viewModel.setBikeCategory(BikeCategory.MTB)
        val currentSetup = viewModel.uiState.value.setups.first()
        viewModel.updateSetups(
            listOf(
                currentSetup.copy(
                    frontTire = currentSetup.frontTire.copy(widthMm = 55.0, isWidthInInches = false),
                    rearTire = currentSetup.rearTire.copy(widthMm = 55.0, isWidthInInches = false)
                )
            )
        )

        var saved = false
        viewModel.saveBike(
            massUnit = MassUnit.KG,
            nameRequiredError = "Name required",
            onSaved = { saved = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved)
        val savedBike = fakeBikeRepo.getAllBikesList().single()
        assertEquals(false, savedBike.setups.first().frontTire.isWidthInInches)
        assertEquals(false, savedBike.setups.first().rearTire.isWidthInInches)
        assertEquals("55mm", savedBike.setups.first().frontTire.formatWidthWithUnit(RideStyle.XC_MTB, " in", "mm"))
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
        override suspend fun reconcileFavorites(mode: com.tirepressure.app.core.model.FavoriteMode) {}
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
}

