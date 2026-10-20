package com.tirepressure.app.ui.bikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.domain.model.*
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.ui.overview.AddSetupDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BikeEditUiState(
    val initialBikeId: String? = null,
    val isLoaded: Boolean = false,
    val currentId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val yearInput: String = "",
    val frameSize: String = "",
    val bikeCategory: BikeCategory = BikeCategory.ROAD,
    val bikeWeightInput: String = "8.0",
    val isDefault: Boolean = false,
    val isGeneralInfoExpanded: Boolean = true,
    val isSetupsExpanded: Boolean = true,
    val isDiameterExpanded: Boolean = true,
    val isFrontWheelExpanded: Boolean = true,
    val isRearWheelExpanded: Boolean = true,
    val setups: List<WheelSetup> = listOf(WheelSetup(name = "Configuration principale", isDefault = true)),
    val selectedSetupIndex: Int = 0,
    val isSameWheels: Boolean = true,
    val showAddSetupDialog: Boolean = false,
    val addSetupDraft: AddSetupDraft? = null,
    val showDeleteBikeConfirmation: Boolean = false,
    val setupToDeleteId: String? = null,
    val nameError: String? = null
)

class BikeEditViewModel(
    private val bikeRepository: BikeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BikeEditUiState())
    val uiState: StateFlow<BikeEditUiState> = _uiState.asStateFlow()

    fun loadBikeIfNeeded(
        bikeId: String,
        massUnit: MassUnit,
        defaultSetupName: String
    ) {
        val current = _uiState.value
        if (current.initialBikeId == bikeId && current.isLoaded) {
            return
        }

        if (bikeId == "new") {
            val defaultWeightInUserUnit = UnitConverter.fromKg(BikeCategory.ROAD.defaultWeightKg, massUnit)
            val weightStr = String.format(java.util.Locale.US, "%.1f", defaultWeightInUserUnit)
            val initialSetup = WheelSetup(
                name = defaultSetupName,
                isDefault = true
            )
            _uiState.value = BikeEditUiState(
                initialBikeId = "new",
                isLoaded = true,
                currentId = UUID.randomUUID().toString(),
                name = "",
                brand = "",
                model = "",
                yearInput = "",
                frameSize = "",
                bikeCategory = BikeCategory.ROAD,
                bikeWeightInput = weightStr,
                isDefault = false,
                isGeneralInfoExpanded = true,
                isSetupsExpanded = true,
                isDiameterExpanded = true,
                isFrontWheelExpanded = true,
                isRearWheelExpanded = true,
                setups = listOf(initialSetup),
                selectedSetupIndex = 0,
                isSameWheels = true
            )
        } else {
            viewModelScope.launch {
                val existing = bikeRepository.getBikeById(bikeId)
                if (existing != null) {
                    val convertedWeight = UnitConverter.fromKg(existing.bikeWeightKg, massUnit)
                    val weightStr = String.format(java.util.Locale.US, "%.1f", convertedWeight)
                    val existingSetups = if (existing.setups.isNotEmpty()) existing.setups else listOf(WheelSetup(name = defaultSetupName, isDefault = true))
                    val firstSetup = existingSetups.first()
                    val sameWheels = firstSetup.frontWheel.diameter == firstSetup.rearWheel.diameter &&
                            firstSetup.frontWheel.innerRimWidthMm == firstSetup.rearWheel.innerRimWidthMm &&
                            firstSetup.frontWheel.rimType == firstSetup.rearWheel.rimType &&
                            firstSetup.frontTire.widthMm == firstSetup.rearTire.widthMm &&
                            firstSetup.frontTire.casing == firstSetup.rearTire.casing

                    _uiState.value = BikeEditUiState(
                        initialBikeId = bikeId,
                        isLoaded = true,
                        currentId = existing.id,
                        name = existing.name,
                        brand = existing.brand,
                        model = existing.model,
                        yearInput = existing.year?.toString() ?: "",
                        frameSize = existing.frameSize,
                        bikeCategory = existing.category,
                        bikeWeightInput = weightStr,
                        isDefault = existing.isDefault,
                        isGeneralInfoExpanded = false,
                        isSetupsExpanded = true,
                        isDiameterExpanded = true,
                        isFrontWheelExpanded = true,
                        isRearWheelExpanded = true,
                        setups = existingSetups,
                        selectedSetupIndex = 0,
                        isSameWheels = sameWheels
                    )
                } else {
                    _uiState.update { it.copy(isLoaded = true, initialBikeId = bikeId) }
                }
            }
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun setBrand(brand: String) {
        _uiState.update { it.copy(brand = brand) }
    }

    fun setModel(model: String) {
        _uiState.update { it.copy(model = model) }
    }

    fun setYearInput(year: String) {
        _uiState.update { it.copy(yearInput = year) }
    }

    fun setFrameSize(frameSize: String) {
        _uiState.update { it.copy(frameSize = frameSize) }
    }

    fun setBikeCategory(category: BikeCategory) {
        _uiState.update { current ->
            val updatedSetups = current.setups.mapIndexed { idx, s ->
                if (idx == 0 && s.rideStyle == null) {
                    val defStyle = category.defaultRideStyle
                    val validDiameters = WheelDiameter.getOptionsForStyle(defStyle)
                    s.copy(
                        frontWheel = s.frontWheel.copy(
                            diameter = if (s.frontWheel.diameter in validDiameters) s.frontWheel.diameter else validDiameters.first(),
                            innerRimWidthMm = defStyle.defaultInnerRimWidthMm,
                            rimType = defStyle.defaultRimType
                        ),
                        rearWheel = s.rearWheel.copy(
                            diameter = if (s.rearWheel.diameter in validDiameters) s.rearWheel.diameter else validDiameters.first(),
                            innerRimWidthMm = defStyle.defaultInnerRimWidthMm,
                            rimType = defStyle.defaultRimType
                        ),
                        frontTire = s.frontTire.copy(
                            widthMm = defStyle.defaultTireWidthMm,
                            casing = defStyle.defaultTireCasing
                        ),
                        rearTire = s.rearTire.copy(
                            widthMm = defStyle.defaultTireWidthMm,
                            casing = defStyle.defaultTireCasing
                        )
                    )
                } else s
            }
            current.copy(bikeCategory = category, setups = updatedSetups)
        }
    }

    fun setBikeWeightInput(weight: String) {
        _uiState.update { it.copy(bikeWeightInput = weight) }
    }

    fun setIsDefault(isDefault: Boolean) {
        _uiState.update { it.copy(isDefault = isDefault) }
    }

    fun toggleGeneralInfoExpanded() {
        _uiState.update { it.copy(isGeneralInfoExpanded = !it.isGeneralInfoExpanded) }
    }

    fun toggleSetupsExpanded() {
        _uiState.update { it.copy(isSetupsExpanded = !it.isSetupsExpanded) }
    }

    fun toggleDiameterExpanded() {
        _uiState.update { it.copy(isDiameterExpanded = !it.isDiameterExpanded) }
    }

    fun toggleFrontWheelExpanded() {
        _uiState.update { it.copy(isFrontWheelExpanded = !it.isFrontWheelExpanded) }
    }

    fun toggleRearWheelExpanded() {
        _uiState.update { it.copy(isRearWheelExpanded = !it.isRearWheelExpanded) }
    }

    fun setSelectedSetupIndex(index: Int) {
        _uiState.update { current ->
            val setup = current.setups.getOrNull(index) ?: return@update current.copy(selectedSetupIndex = index)
            val sameWheels = setup.frontWheel.diameter == setup.rearWheel.diameter &&
                    setup.frontWheel.innerRimWidthMm == setup.rearWheel.innerRimWidthMm &&
                    setup.frontWheel.rimType == setup.rearWheel.rimType &&
                    setup.frontTire.widthMm == setup.rearTire.widthMm &&
                    setup.frontTire.casing == setup.rearTire.casing
            current.copy(selectedSetupIndex = index, isSameWheels = sameWheels)
        }
    }

    fun setIsSameWheels(same: Boolean) {
        _uiState.update { it.copy(isSameWheels = same) }
    }

    fun updateSetups(newSetups: List<WheelSetup>) {
        _uiState.update { it.copy(setups = newSetups) }
    }

    fun updateActiveSetup(transform: (WheelSetup) -> WheelSetup) {
        _uiState.update { current ->
            val updated = current.setups.mapIndexed { idx, s ->
                if (idx == current.selectedSetupIndex) transform(s) else s
            }
            current.copy(setups = updated)
        }
    }

    fun setShowAddSetupDialog(show: Boolean) {
        _uiState.update { it.copy(showAddSetupDialog = show) }
    }

    fun startAddSetup(defaultSetupPrefix: String) {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                showAddSetupDialog = true,
                addSetupDraft = AddSetupDraft(
                    bikeId = current.currentId,
                    bikeName = current.name.ifBlank { "Vélo" },
                    bikeWeightKg = 8.0,
                    bikeCategory = current.bikeCategory,
                    setupName = "$defaultSetupPrefix ${current.setups.size + 1}",
                    useExistingTemplate = current.setups.isNotEmpty(),
                    selectedTemplateIndex = 0,
                    setupRideStyle = if (current.setups.isNotEmpty()) current.setups[0].getEffectiveRideStyle(current.bikeCategory)
                    else current.bikeCategory.defaultRideStyle
                )
            )
        }
    }

    fun updateAddSetupDraft(draft: AddSetupDraft) {
        _uiState.update { it.copy(addSetupDraft = draft) }
    }

    fun cancelAddSetup() {
        _uiState.update { it.copy(showAddSetupDialog = false, addSetupDraft = null) }
    }

    fun confirmAddSetup(
        newSetupName: String,
        templateSetup: WheelSetup?,
        overrideRideStyle: RideStyle?,
        defaultSetupPrefix: String
    ) {
        val current = _uiState.value
        val newSetup = if (templateSetup != null) {
            templateSetup.copy(
                id = UUID.randomUUID().toString(),
                name = newSetupName.ifBlank { "$defaultSetupPrefix ${current.setups.size + 1}" },
                rideStyle = overrideRideStyle ?: templateSetup.rideStyle ?: current.bikeCategory.defaultRideStyle,
                isDefault = false
            )
        } else {
            val targetStyle = overrideRideStyle ?: current.bikeCategory.defaultRideStyle
            val validDiameters = WheelDiameter.getOptionsForStyle(targetStyle)
            WheelSetup(
                id = UUID.randomUUID().toString(),
                name = newSetupName.ifBlank { "$defaultSetupPrefix ${current.setups.size + 1}" },
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
        val updatedSetups = current.setups + newSetup
        _uiState.update {
            it.copy(
                setups = updatedSetups,
                selectedSetupIndex = updatedSetups.size - 1,
                showAddSetupDialog = false,
                addSetupDraft = null
            )
        }
    }

    fun setShowDeleteBikeConfirmation(show: Boolean) {
        _uiState.update { it.copy(showDeleteBikeConfirmation = show) }
    }

    fun setSetupToDeleteId(id: String?) {
        _uiState.update { it.copy(setupToDeleteId = id) }
    }

    fun confirmDeleteSetup() {
        val current = _uiState.value
        val toDelete = current.setupToDeleteId ?: return
        if (current.setups.size <= 1) {
            _uiState.update { it.copy(setupToDeleteId = null) }
            return
        }
        val updated = current.setups.filter { it.id != toDelete }
        _uiState.update {
            it.copy(
                setups = updated,
                selectedSetupIndex = 0,
                setupToDeleteId = null
            )
        }
    }

    fun setNameError(error: String?) {
        _uiState.update { it.copy(nameError = error) }
    }

    fun saveBike(
        massUnit: MassUnit,
        nameRequiredError: String,
        onSaved: () -> Unit
    ) {
        val current = _uiState.value
        if (current.name.isBlank()) {
            _uiState.update { it.copy(nameError = nameRequiredError) }
            return
        }
        viewModelScope.launch {
            val typedBikeWeight = current.bikeWeightInput.toDoubleOrNull() ?: UnitConverter.fromKg(8.0, massUnit)
            val bikeWeightKg = UnitConverter.toKg(typedBikeWeight, massUnit)
            val bike = BikeProfile(
                id = current.currentId,
                name = current.name.trim(),
                brand = current.brand.trim(),
                model = current.model.trim(),
                year = current.yearInput.toIntOrNull(),
                frameSize = current.frameSize.trim(),
                category = current.bikeCategory,
                bikeWeightKg = bikeWeightKg,
                setups = current.setups,
                isDefault = current.isDefault,
                updatedAt = System.currentTimeMillis()
            )
            val existing = bikeRepository.getBikeById(bike.id)
            if (existing == null) {
                bikeRepository.insertBike(bike)
            } else {
                bikeRepository.updateBike(bike)
            }
            reset()
            onSaved()
        }
    }

    fun deleteBike(onDeleted: () -> Unit) {
        val current = _uiState.value
        viewModelScope.launch {
            bikeRepository.deleteBike(current.currentId)
            reset()
            onDeleted()
        }
    }

    fun reset() {
        _uiState.value = BikeEditUiState()
    }
}
