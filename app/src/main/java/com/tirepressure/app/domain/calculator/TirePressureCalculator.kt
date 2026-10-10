package com.tirepressure.app.domain.calculator

import com.tirepressure.app.domain.model.PressureCalculationInput
import com.tirepressure.app.domain.model.PressureCalculationResult

interface TirePressureCalculator {
    /**
     * Calcule les pressions recommandées pour les roues avant et arrière.
     * @param input Données d'entrées validées.
     * @return Résultat structuré contenant les pressions et les avertissements éventuels.
     */
    fun calculate(input: PressureCalculationInput): PressureCalculationResult
}

