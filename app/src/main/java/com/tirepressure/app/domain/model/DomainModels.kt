package com.tirepressure.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TireConfig(
    val widthMm: Double,
    val casing: TireCasing = TireCasing.STANDARD
)

@Serializable
data class WheelConfig(
    val diameter: WheelDiameter = WheelDiameter.DIAMETER_622,
    val innerRimWidthMm: Double = 21.0,
    val rimType: RimType = RimType.TUBELESS_STRAIGHT_SIDE
)

@Serializable
data class WheelSetup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rideStyle: RideStyle? = null,
    val frontTire: TireConfig = TireConfig(28.0),
    val rearTire: TireConfig = TireConfig(28.0),
    val frontWheel: WheelConfig = WheelConfig(),
    val rearWheel: WheelConfig = WheelConfig(),
    val luggageWeightKg: Double = 0.0,
    val isDefault: Boolean = false,
    val showDry: Boolean = true,
    val showWet: Boolean = true,
    val showSnow: Boolean = true
) {
    fun getEffectiveRideStyle(bikeCategory: BikeCategory): RideStyle =
        rideStyle ?: bikeCategory.defaultRideStyle

    fun getEffectiveRideStyle(bikeStyle: RideStyle): RideStyle =
        rideStyle ?: bikeStyle
}

@Serializable
data class BikeProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String = "",
    val model: String = "",
    val year: Int? = null,
    val frameSize: String = "",
    val category: BikeCategory = BikeCategory.ROAD,
    val bikeWeightKg: Double = 8.0,
    val setups: List<WheelSetup> = listOf(
        WheelSetup(
            name = "Configuration principale",
            isDefault = true
        )
    ),
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val rideStyle: RideStyle
        get() = category.defaultRideStyle

    val primarySetup: WheelSetup
        get() = setups.find { it.isDefault } ?: setups.firstOrNull() ?: WheelSetup(name = "Par défaut")
}

@Serializable
data class PressureCalculationInput(
    val riderWeightKg: Double,
    val bikeWeightKg: Double,
    val luggageWeightKg: Double = 0.0,
    val rideStyle: RideStyle,
    val surfaceCondition: SurfaceCondition = SurfaceCondition.DRY,
    val frontTire: TireConfig,
    val rearTire: TireConfig,
    val frontWheel: WheelConfig,
    val rearWheel: WheelConfig
) {
    val totalWeightKg: Double
        get() = riderWeightKg + bikeWeightKg + luggageWeightKg
}

@Serializable
data class WheelPressureResult(
    val pressureMbar: Double,
    val pressureBar: Double,
    val pressurePsi: Double,
    val pressureKpa: Double,
    val isHooklessExceeded: Boolean,
    val hooklessMaxThresholdBar: Double?,
    val isIncompatibleRimTire: Boolean
)

@Serializable
data class PressureCalculationResult(
    val front: WheelPressureResult,
    val rear: WheelPressureResult,
    val totalSystemWeightKg: Double,
    val rideStyle: RideStyle,
    val surfaceCondition: SurfaceCondition,
    val hasHooklessWarning: Boolean,
    val hasCompatibilityWarning: Boolean
)

data class SetupMultiSurfaceResult(
    val setup: WheelSetup,
    val dryResult: PressureCalculationResult,
    val wetResult: PressureCalculationResult,
    val snowResult: PressureCalculationResult
)

data class BikeOverviewData(
    val bike: BikeProfile,
    val setupsWithPressures: List<SetupMultiSurfaceResult>
)

data class ValidationError(
    val field: String,
    val message: String
)

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList()
)
