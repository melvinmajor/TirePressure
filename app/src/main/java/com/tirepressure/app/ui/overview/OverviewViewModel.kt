package com.tirepressure.app.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.calculator.TirePressureCalculator
import com.tirepressure.app.domain.model.*
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class EditBikeDraft(
    val bikeId: String,
    val name: String,
    val brand: String = "",
    val model: String = "",
    val yearInput: String = "",
    val frameSize: String = "",
    val weightInput: String = "",
    val category: BikeCategory = BikeCategory.ROAD,
    val isDefault: Boolean = false,
    val nameError: String? = null
)

data class AddSetupDraft(
    val bikeId: String,
    val bikeName: String,
    val bikeWeightKg: Double = 8.0,
    val bikeCategory: BikeCategory = BikeCategory.ROAD,
    val setupName: String = "",
    val useExistingTemplate: Boolean = true,
    val selectedTemplateIndex: Int = 0,
    val setupRideStyle: RideStyle? = null
)

data class OverviewUiState(
    val bikesWithPressures: List<BikeOverviewData> = emptyList(),
    val riderWeightKg: Double = 75.0,
    val massUnit: MassUnit = MassUnit.KG,
    val pressureUnit: PressureUnit = PressureUnit.BAR,
    val showWetSurface: Boolean = true,
    val showSnowSurface: Boolean = false,
    val defaultOverviewCompact: Boolean = false,
    val isLoading: Boolean = false,
    val bikeToDelete: BikeProfile? = null,
    val setupToDelete: Pair<BikeProfile, WheelSetup>? = null,
    val editingBikeDraft: EditBikeDraft? = null,
    val addSetupDraft: AddSetupDraft? = null
)

class OverviewViewModel(
    private val bikeRepository: BikeRepository,
    private val preferencesRepository: PreferencesRepository,
    private val calculator: TirePressureCalculator = BertoTirePressureCalculator()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bikeRepository.getAllBikes(),
                preferencesRepository.riderWeightFlow,
                preferencesRepository.massUnitFlow,
                preferencesRepository.pressureUnitFlow,
                combine(
                    preferencesRepository.showWetSurfaceFlow,
                    preferencesRepository.showSnowSurfaceFlow,
                    preferencesRepository.defaultOverviewCompactFlow
                ) { wet, snow, defaultCompact -> Triple(wet, snow, defaultCompact) }
            ) { bikes, riderWeight, massUnit, pressureUnit, surfacesAndCompact ->
                val (showWet, showSnow, defaultCompact) = surfacesAndCompact
                val list = bikes.map { bike ->
                    val setupsWithPressures = bike.setups.map { setup ->
                        val effectiveStyle = setup.getEffectiveRideStyle(bike.category)
                        val baseInput = PressureCalculationInput(
                            riderWeightKg = riderWeight,
                            bikeWeightKg = bike.bikeWeightKg,
                            luggageWeightKg = setup.luggageWeightKg,
                            rideStyle = effectiveStyle,
                            frontTire = setup.frontTire,
                            rearTire = setup.rearTire,
                            frontWheel = setup.frontWheel,
                            rearWheel = setup.rearWheel
                        )

                        val dryRes = calculator.calculate(baseInput.copy(surfaceCondition = SurfaceCondition.DRY))
                        val wetRes = calculator.calculate(baseInput.copy(surfaceCondition = SurfaceCondition.WET))
                        val snowRes = calculator.calculate(baseInput.copy(surfaceCondition = SurfaceCondition.SNOW))

                        SetupMultiSurfaceResult(
                            setup = setup,
                            dryResult = dryRes,
                            wetResult = wetRes,
                            snowResult = snowRes
                        )
                    }

                    BikeOverviewData(
                        bike = bike,
                        setupsWithPressures = setupsWithPressures
                    )
                }

                _uiState.update { current ->
                    current.copy(
                        bikesWithPressures = list,
                        riderWeightKg = riderWeight,
                        massUnit = massUnit,
                        pressureUnit = pressureUnit,
                        showWetSurface = showWet,
                        showSnowSurface = showSnow,
                        defaultOverviewCompact = defaultCompact,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun startEditBikeInfo(bike: BikeProfile, massUnit: MassUnit) {
        val initialWeightInUserUnit = UnitConverter.fromKg(bike.bikeWeightKg, massUnit)
        val weightStr = String.format(java.util.Locale.US, "%.1f", initialWeightInUserUnit).replace(',', '.')
        _uiState.update {
            it.copy(
                editingBikeDraft = EditBikeDraft(
                    bikeId = bike.id,
                    name = bike.name,
                    brand = bike.brand,
                    model = bike.model,
                    yearInput = bike.year?.toString() ?: "",
                    frameSize = bike.frameSize,
                    weightInput = weightStr,
                    category = bike.category,
                    isDefault = bike.isDefault
                )
            )
        }
    }

    fun updateEditBikeDraft(draft: EditBikeDraft) {
        _uiState.update { it.copy(editingBikeDraft = draft) }
    }

    fun cancelEditBikeInfo() {
        _uiState.update { it.copy(editingBikeDraft = null) }
    }

    fun confirmEditBikeInfo(updatedBike: BikeProfile) {
        viewModelScope.launch {
            bikeRepository.updateBike(updatedBike)
            _uiState.update { it.copy(editingBikeDraft = null) }
        }
    }

    fun startAddSetup(bike: BikeProfile, defaultSetupPrefix: String) {
        _uiState.update {
            it.copy(
                addSetupDraft = AddSetupDraft(
                    bikeId = bike.id,
                    bikeName = bike.name,
                    bikeWeightKg = bike.bikeWeightKg,
                    bikeCategory = bike.category,
                    setupName = "$defaultSetupPrefix ${bike.setups.size + 1}",
                    useExistingTemplate = bike.setups.isNotEmpty(),
                    selectedTemplateIndex = 0,
                    setupRideStyle = if (bike.setups.isNotEmpty()) bike.setups[0].getEffectiveRideStyle(bike.category)
                    else bike.category.defaultRideStyle
                )
            )
        }
    }

    fun updateAddSetupDraft(draft: AddSetupDraft) {
        _uiState.update { it.copy(addSetupDraft = draft) }
    }

    fun cancelAddSetup() {
        _uiState.update { it.copy(addSetupDraft = null) }
    }

    fun setPressureUnit(unit: PressureUnit) {
        viewModelScope.launch {
            preferencesRepository.setPressureUnit(unit)
        }
    }

    fun requestDeleteBike(bike: BikeProfile) {
        _uiState.update { it.copy(bikeToDelete = bike) }
    }

    fun cancelDeleteBike() {
        _uiState.update { it.copy(bikeToDelete = null) }
    }

    fun confirmDeleteBike() {
        val bike = _uiState.value.bikeToDelete ?: return
        viewModelScope.launch {
            bikeRepository.deleteBike(bike.id)
            _uiState.update { it.copy(bikeToDelete = null) }
        }
    }

    fun updateBike(bike: BikeProfile) {
        viewModelScope.launch {
            bikeRepository.updateBike(bike)
        }
    }

    fun toggleFavoriteBike(bike: BikeProfile) {
        viewModelScope.launch {
            bikeRepository.toggleFavoriteBike(bike.id)
        }
    }

    fun duplicateBike(bike: BikeProfile, copySuffix: String = " (copy)") {
        viewModelScope.launch {
            val duplicate = bike.copy(
                id = UUID.randomUUID().toString(),
                name = "${bike.name} $copySuffix".trim(),
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            bikeRepository.insertBike(duplicate)
        }
    }

    fun addSetupToBike(
        bike: BikeProfile,
        setupName: String,
        templateSetup: WheelSetup? = null,
        overrideRideStyle: RideStyle? = null,
        defaultNamePrefix: String = "Setup"
    ) {
        viewModelScope.launch {
            val newSetup = if (templateSetup != null) {
                templateSetup.copy(
                    id = UUID.randomUUID().toString(),
                    name = setupName.ifBlank { "$defaultNamePrefix ${bike.setups.size + 1}" },
                    rideStyle = overrideRideStyle ?: templateSetup.rideStyle ?: bike.category.defaultRideStyle,
                    isDefault = false
                )
            } else {
                val targetStyle = overrideRideStyle ?: bike.category.defaultRideStyle
                val validDiameters = WheelDiameter.getOptionsForStyle(targetStyle)
                WheelSetup(
                    id = UUID.randomUUID().toString(),
                    name = setupName.ifBlank { "$defaultNamePrefix ${bike.setups.size + 1}" },
                    rideStyle = targetStyle,
                    frontTire = TireConfig(
                        widthMm = targetStyle.defaultTireWidthMm,
                        casing = targetStyle.defaultTireCasing
                    ),
                    rearTire = TireConfig(
                        widthMm = targetStyle.defaultTireWidthMm,
                        casing = targetStyle.defaultTireCasing
                    ),
                    frontWheel = WheelConfig(
                        diameter = validDiameters.first(),
                        innerRimWidthMm = targetStyle.defaultInnerRimWidthMm,
                        rimType = targetStyle.defaultRimType
                    ),
                    rearWheel = WheelConfig(
                        diameter = validDiameters.first(),
                        innerRimWidthMm = targetStyle.defaultInnerRimWidthMm,
                        rimType = targetStyle.defaultRimType
                    ),
                    luggageWeightKg = 0.0,
                    isDefault = false
                )
            }
            val updatedBike = bike.copy(
                setups = bike.setups + newSetup,
                updatedAt = System.currentTimeMillis()
            )
            bikeRepository.updateBike(updatedBike)
        }
    }

    fun duplicateSetup(bike: BikeProfile, setup: WheelSetup, copySuffix: String = " (copy)") {
        viewModelScope.launch {
            val dupSetup = setup.copy(
                id = UUID.randomUUID().toString(),
                name = "${setup.name} $copySuffix".trim(),
                isDefault = false
            )
            val updatedBike = bike.copy(
                setups = bike.setups + dupSetup,
                updatedAt = System.currentTimeMillis()
            )
            bikeRepository.updateBike(updatedBike)
        }
    }

    fun requestDeleteSetup(bike: BikeProfile, setup: WheelSetup) {
        _uiState.update { it.copy(setupToDelete = Pair(bike, setup)) }
    }

    fun cancelDeleteSetup() {
        _uiState.update { it.copy(setupToDelete = null) }
    }

    fun confirmDeleteSetup() {
        val pair = _uiState.value.setupToDelete ?: return
        val (bike, setup) = pair
        if (bike.setups.size <= 1) {
            _uiState.update { it.copy(setupToDelete = null) }
            return
        }
        viewModelScope.launch {
            val updated = bike.copy(
                setups = bike.setups.filterNot { it.id == setup.id },
                updatedAt = System.currentTimeMillis()
            )
            bikeRepository.updateBike(updated)
            _uiState.update { it.copy(setupToDelete = null) }
        }
    }

    fun deleteSetup(bike: BikeProfile, setupId: String) {
        if (bike.setups.size <= 1) return // Conserver au moins une configuration
        viewModelScope.launch {
            val updated = bike.copy(
                setups = bike.setups.filterNot { it.id == setupId },
                updatedAt = System.currentTimeMillis()
            )
            bikeRepository.updateBike(updated)
        }
    }
}

