package com.tirepressure.app.domain.calculator

import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.domain.model.*
import kotlin.math.PI
import kotlin.math.pow

/**
 * Calculateur de pression de pneumatiques cyclistes open source fondé sur la science physique du pneu :
 * 1. Le principe du fléchissement optimal (Tire Drop) de Frank Berto et la modélisation du volume toroïdal d'air.
 * 2. La modélisation de l'impédance de roulement, de la dynamique de discipline et du transfert de charge.
 * 3. Les normes internationales de sécurité ISO 5775 et ETRTO pour jantes sans crochets (Hookless).
 *
 * Ce modèle est 100% libre de droits, transparent, auditable et sans dépendance propriétaire.
 */
class BertoTirePressureCalculator : TirePressureCalculator {

    companion object {
        // Multiplicateurs dynamiques de discipline
        fun getDisciplineMultiplier(discipline: RideStyle, tireWidthMm: Double): Double {
            return when (discipline) {
                RideStyle.ROAD -> if (tireWidthMm > 40.0) 1.2541 else 1.00
                RideStyle.XC_MTB -> 0.945
                RideStyle.ENDURO_MTB -> 1.10
                RideStyle.TRAIL_MTB -> 1.05
                RideStyle.DOWNHILL_MTB -> 1.15
                RideStyle.GRAVEL -> 0.90
                RideStyle.CYCLOCROSS -> 0.75
                RideStyle.FAT_BIKE -> 0.85
                RideStyle.COMMUTE -> 1.05
            }
        }

        // Multiplicateurs de type de jante et montage
        val RIM_TYPE_MULTIPLIERS = mapOf(
            RimType.TUBELESS_CROCHET to 1.03,
            RimType.TUBELESS_STRAIGHT_SIDE to 0.955,
            RimType.TUBES to 1.05,
            RimType.TUBULAR to 1.10,
            RimType.WIDE_AERO_HOOKLESS to 0.955
        )

        // Multiplicateurs de structure de carcasse
        val CASING_MULTIPLIERS = mapOf(
            TireCasing.STANDARD to 1.00,
            TireCasing.THIN to 1.025,
            TireCasing.REINFORCED to 0.95,
            TireCasing.DOUBLE to 0.90
        )

        // Multiplicateurs de surface et météo
        val SURFACE_MULTIPLIERS = mapOf(
            SurfaceCondition.DRY to 1.00,
            SurfaceCondition.WET to 0.90,
            SurfaceCondition.SNOW to 0.50
        )

        // Tableau de correspondance ISO/ETRTO pour la largeur interne de référence
        private data class RimReference(val minTireMm: Double, val maxTireMm: Double, val compatibleRimWidth: Double)
        private val RIM_REFERENCE_TABLE = listOf(
            RimReference(18.0, 22.0, 15.0),
            RimReference(22.0, 25.0, 17.0),
            RimReference(25.0, 29.0, 19.0),
            RimReference(29.0, 35.0, 21.0),
            RimReference(35.0, 47.0, 23.0),
            RimReference(47.0, 58.0, 25.0),
            RimReference(58.0, 66.0, 30.0),
            RimReference(66.0, 72.0, 35.0),
            RimReference(72.0, 84.0, 45.0),
            RimReference(84.0, 96.0, 55.0),
            RimReference(96.0, 113.0, 76.0),
            RimReference(114.0, 133.0, 94.0)
        )

        fun getReferenceRimWidth(tireWidthMm: Double): Double {
            val found = RIM_REFERENCE_TABLE.find { tireWidthMm >= it.minTireMm && tireWidthMm < it.maxTireMm }
            return found?.compatibleRimWidth ?: 25.0
        }

        // Limites maximales de sécurité pour jantes Hookless sans crochets (Normes ETRTO / ISO 5775)
        private data class HooklessLimit(val minTireMm: Double, val maxTireMm: Double, val maxPressureMbar: Double)
        private val HOOKLESS_SAFETY_LIMITS = listOf(
            HooklessLimit(18.0, 24.0, 5500.0), // 5.5 bar max
            HooklessLimit(25.0, 29.0, 5000.0), // 5.0 bar (72.5 psi) - Seuil d'or ETRTO
            HooklessLimit(30.0, 34.0, 4500.0), // 4.5 bar
            HooklessLimit(35.0, 39.0, 4000.0), // 4.0 bar
            HooklessLimit(40.0, 44.0, 3500.0), // 3.5 bar
            HooklessLimit(45.0, 54.0, 3000.0), // 3.0 bar
            HooklessLimit(55.0, 69.0, 2500.0), // 2.5 bar
            HooklessLimit(70.0, 89.0, 2000.0), // 2.0 bar
            HooklessLimit(90.0, 140.0, 1500.0) // 1.5 bar
        )

        // Compatibilité largeur jante / pneu ISO 5775
        private data class RimCompatibility(val minRim: Double, val maxRim: Double, val minTire: Double, val maxTire: Double)
        private val RIM_COMPATIBILITY_TABLE = listOf(
            RimCompatibility(19.0, 21.0, 25.0, 50.0),
            RimCompatibility(22.0, 25.0, 28.0, 65.0),
            RimCompatibility(26.0, 30.0, 35.0, 75.0),
            RimCompatibility(31.0, 35.0, 50.0, 85.0),
            RimCompatibility(36.0, 45.0, 60.0, 100.0),
            RimCompatibility(46.0, 100.0, 80.0, 135.0)
        )
    }

    override fun calculate(input: PressureCalculationInput): PressureCalculationResult {
        val totalWeightKg = input.riderWeightKg + input.bikeWeightKg + input.luggageWeightKg

        // Calcul Roue Avant
        val frontResult = calculateWheelPressure(
            totalWeightKg = totalWeightKg,
            discipline = input.rideStyle,
            tireConfig = input.frontTire,
            wheelConfig = input.frontWheel,
            surface = input.surfaceCondition,
            isFront = true
        )

        // Calcul Roue Arrière
        val rearResult = calculateWheelPressure(
            totalWeightKg = totalWeightKg,
            discipline = input.rideStyle,
            tireConfig = input.rearTire,
            wheelConfig = input.rearWheel,
            surface = input.surfaceCondition,
            isFront = false
        )

        val hasHooklessWarning = frontResult.isHooklessExceeded || rearResult.isHooklessExceeded
        val hasCompatibilityWarning = frontResult.isIncompatibleRimTire || rearResult.isIncompatibleRimTire

        return PressureCalculationResult(
            front = frontResult,
            rear = rearResult,
            totalSystemWeightKg = totalWeightKg,
            rideStyle = input.rideStyle,
            surfaceCondition = input.surfaceCondition,
            hasHooklessWarning = hasHooklessWarning,
            hasCompatibilityWarning = hasCompatibilityWarning
        )
    }

    private fun calculateWheelPressure(
        totalWeightKg: Double,
        discipline: RideStyle,
        tireConfig: TireConfig,
        wheelConfig: WheelConfig,
        surface: SurfaceCondition,
        isFront: Boolean
    ): WheelPressureResult {
        // 1. Largeur effective réelle du pneu sur la jante interne (ajustement ISO/ETRTO)
        val refRimWidth = getReferenceRimWidth(tireConfig.widthMm)
        val rimDelta = wheelConfig.innerRimWidthMm - refRimWidth
        val effectiveTireWidth = tireConfig.widthMm + 0.4 * rimDelta

        // 2. Modèle toroïdal physique du volume d'air :
        // c = 4 * PI^2 * (diameter/2 + s/2) * (s/2)
        val diameterMm = wheelConfig.diameter.diameterMm
        val torusC = 4.0 * PI.pow(2) * (diameterMm / 2.0 + effectiveTireWidth / 2.0) * (effectiveTireWidth / 2.0)

        // Facteur de charge globale du système (cycliste + vélo + bagages)
        val weightFactor = 1.0 + (2.2 * totalWeightKg - 180.0) * 0.0025
        val wheelPositionMultiplier = if (isFront) 0.94 else 1.00

        // Pression de base physique
        val basePsi = 10.0.pow(8.684670773) * torusC.pow(-1.304556655) * weightFactor * wheelPositionMultiplier

        // 3. Application des coefficients d'ajustement physique et matériel
        val disciplineMultiplier = getDisciplineMultiplier(discipline, tireConfig.widthMm)
        val rimMultiplier = RIM_TYPE_MULTIPLIERS[wheelConfig.rimType] ?: 1.0
        val casingMultiplier = CASING_MULTIPLIERS[tireConfig.casing] ?: 1.0
        val surfaceMultiplier = SURFACE_MULTIPLIERS[surface] ?: 1.0

        val finalPsi = basePsi * disciplineMultiplier * rimMultiplier * casingMultiplier * surfaceMultiplier

        // 4. Conversions précises
        val pressureMbar = finalPsi * UnitConverter.PSI_TO_MBAR
        val pressureBar = UnitConverter.fromMbar(pressureMbar, PressureUnit.BAR)
        val pressurePsi = UnitConverter.fromMbar(pressureMbar, PressureUnit.PSI)
        val pressureKpa = UnitConverter.fromMbar(pressureMbar, PressureUnit.KPA)

        // 5. Vérification du plafond de sécurité Hookless (ETRTO)
        var isHooklessExceeded = false
        var hooklessThresholdBar: Double? = null
        if (wheelConfig.rimType.isHookless) {
            val limit = HOOKLESS_SAFETY_LIMITS.find {
                tireConfig.widthMm >= it.minTireMm && tireConfig.widthMm <= it.maxTireMm
            }
            if (limit != null) {
                hooklessThresholdBar = limit.maxPressureMbar / 1000.0
                if (pressureMbar > limit.maxPressureMbar) {
                    isHooklessExceeded = true
                }
            }
        }

        // 6. Vérification de la compatibilité largeur jante / pneu
        val isIncompatible = checkRimCompatibility(wheelConfig.innerRimWidthMm, tireConfig.widthMm)

        return WheelPressureResult(
            pressureMbar = pressureMbar,
            pressureBar = pressureBar,
            pressurePsi = pressurePsi,
            pressureKpa = pressureKpa,
            isHooklessExceeded = isHooklessExceeded,
            hooklessMaxThresholdBar = hooklessThresholdBar,
            isIncompatibleRimTire = isIncompatible
        )
    }

    private fun checkRimCompatibility(innerRimWidthMm: Double, tireWidthMm: Double): Boolean {
        if (innerRimWidthMm >= tireWidthMm) return true
        val rule = RIM_COMPATIBILITY_TABLE.find {
            innerRimWidthMm >= it.minRim && innerRimWidthMm <= it.maxRim
        }
        return if (rule != null) {
            tireWidthMm < rule.minTire || tireWidthMm > rule.maxTire
        } else {
            false
        }
    }
}
