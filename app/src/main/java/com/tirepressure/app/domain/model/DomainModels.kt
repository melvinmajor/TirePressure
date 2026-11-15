package com.tirepressure.app.domain.model

import com.tirepressure.app.core.util.UnitConverter
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TireConfig(
    val widthMm: Double,
    val casing: TireCasing = TireCasing.STANDARD,
    val isWidthInInches: Boolean? = null
) {
    /**
     * Résout l'unité d'affichage de largeur de pneu pour ce pneu :
     * utilise le choix explicitement enregistré (`isWidthInInches`) s'il est défini,
     * sinon déduit selon le style de pratique effectif ou le seuil historique (>= 50 mm).
     */
    fun resolveUseInches(effectiveRideStyle: RideStyle? = null): Boolean {
        isWidthInInches?.let { return it }
        if (effectiveRideStyle != null) {
            return effectiveRideStyle.isMtbInches
        }
        val inInches = widthMm / 25.4
        val nearestTenth = Math.round(inInches * 10.0) / 10.0
        return widthMm >= 50.0 && Math.abs(inInches - nearestTenth) < 0.08
    }

    /**
     * Retourne la largeur numérique convertie dans l'unité résolue (pouces ou mm)
     * et formatée avec au maximum 2 chiffres après la virgule.
     */
    fun formatWidthNumber(effectiveRideStyle: RideStyle? = null): String {
        val raw = if (resolveUseInches(effectiveRideStyle)) widthMm / 25.4 else widthMm
        return UnitConverter.formatDecimal(raw, 2)
    }

    /**
     * Formate la largeur avec son suffixe d'unité (ex: `2.35"`, `28mm`).
     */
    fun formatWidthWithUnit(effectiveRideStyle: RideStyle? = null, inchUnitLabel: String = "in", mmUnitLabel: String = "mm"): String {
        val useInches = resolveUseInches(effectiveRideStyle)
        val number = formatWidthNumber(effectiveRideStyle)
        return if (useInches) "$number$inchUnitLabel" else "$number$mmUnitLabel"
    }
}

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

    fun isEffectiveDefault(allSetups: List<WheelSetup>): Boolean {
        val explicitDefault = allSetups.firstOrNull { it.isDefault }
        return explicitDefault?.id == id || (explicitDefault == null && allSetups.firstOrNull()?.id == id)
    }
}

fun List<WheelSetup>.withSingleDefault(setupId: String? = null): List<WheelSetup> {
    if (isEmpty()) return this
    val targetId = when {
        setupId != null && any { it.id == setupId } -> setupId
        else -> firstOrNull { it.isDefault }?.id ?: first().id
    }
    return map { it.copy(isDefault = it.id == targetId) }
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

    val sortedSetups: List<WheelSetup>
        get() {
            val defaultSetup = setups.firstOrNull { it.isDefault } ?: return setups
            return listOf(defaultSetup) + setups.filter { it.id != defaultSetup.id }
        }
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
