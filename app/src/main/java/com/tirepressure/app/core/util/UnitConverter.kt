package com.tirepressure.app.core.util

import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

object UnitConverter {
    // Constante physique internationale : 1 psi = 68.9475729 mbar
    const val PSI_TO_MBAR = 68.9475729
    const val KG_TO_LBS = 2.20462262185

    fun fromMbar(pressureInMbar: Double, targetUnit: PressureUnit): Double {
        return when (targetUnit) {
            PressureUnit.BAR -> pressureInMbar / 1000.0
            PressureUnit.PSI -> pressureInMbar / PSI_TO_MBAR
            PressureUnit.KPA -> pressureInMbar / 10.0 // 1000 mbar = 100 kPa -> 1 mbar = 0.1 kPa
        }
    }

    fun toMbar(pressureValue: Double, fromUnit: PressureUnit): Double {
        return when (fromUnit) {
            PressureUnit.BAR -> pressureValue * 1000.0
            PressureUnit.PSI -> pressureValue * PSI_TO_MBAR
            PressureUnit.KPA -> pressureValue * 10.0
        }
    }

    fun fromKg(weightInKg: Double, targetUnit: MassUnit): Double {
        return when (targetUnit) {
            MassUnit.KG -> weightInKg
            MassUnit.LB -> weightInKg * KG_TO_LBS
        }
    }

    fun toKg(weightValue: Double, fromUnit: MassUnit): Double {
        return when (fromUnit) {
            MassUnit.KG -> weightValue
            MassUnit.LB -> weightValue / KG_TO_LBS
        }
    }

    fun round(value: Double, decimals: Int): Double {
        if (!value.isFinite()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}

