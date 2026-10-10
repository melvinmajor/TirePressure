package com.tirepressure.app.domain.calculator

import com.tirepressure.app.domain.model.PressureCalculationInput
import com.tirepressure.app.domain.model.ValidationError
import com.tirepressure.app.domain.model.ValidationResult

object TirePressureValidator {

    fun validate(input: PressureCalculationInput): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // 1. Poids du cycliste
        if (input.riderWeightKg <= 0.0) {
            errors.add(ValidationError("riderWeight", "Le poids du cycliste doit être supérieur à 0."))
        } else if (input.riderWeightKg < 30.0 || input.riderWeightKg > 200.0) {
            errors.add(ValidationError("riderWeight", "Le poids du cycliste doit être compris entre 30 et 200 kg."))
        }

        // 2. Poids du vélo
        if (input.bikeWeightKg <= 0.0) {
            errors.add(ValidationError("bikeWeight", "Le poids du vélo doit être supérieur à 0."))
        } else if (input.bikeWeightKg < 4.0 || input.bikeWeightKg > 40.0) {
            errors.add(ValidationError("bikeWeight", "Le poids du vélo doit être compris entre 4 et 40 kg."))
        }

        // 3. Poids des bagages
        if (input.luggageWeightKg < 0.0 || input.luggageWeightKg > 60.0) {
            errors.add(ValidationError("luggageWeight", "Le poids des bagages doit être compris entre 0 et 60 kg."))
        }

        // 4. Largeur pneu avant
        if (input.frontTire.widthMm <= 0.0) {
            errors.add(ValidationError("frontTireWidth", "La largeur du pneu avant est requise."))
        } else if (input.frontTire.widthMm < 18.0 || input.frontTire.widthMm > 135.0) {
            errors.add(ValidationError("frontTireWidth", "La largeur du pneu avant doit être comprise entre 18 et 135 mm."))
        }

        // 5. Largeur pneu arrière
        if (input.rearTire.widthMm <= 0.0) {
            errors.add(ValidationError("rearTireWidth", "La largeur du pneu arrière est requise."))
        } else if (input.rearTire.widthMm < 18.0 || input.rearTire.widthMm > 135.0) {
            errors.add(ValidationError("rearTireWidth", "La largeur du pneu arrière doit être comprise entre 18 et 135 mm."))
        }

        // 6. Largeur interne jante avant
        if (input.frontWheel.innerRimWidthMm <= 0.0) {
            errors.add(ValidationError("frontRimWidth", "La largeur interne de la jante avant est requise."))
        } else if (input.frontWheel.innerRimWidthMm < 13.0 || input.frontWheel.innerRimWidthMm > 110.0) {
            errors.add(ValidationError("frontRimWidth", "La largeur interne de la jante avant doit être comprise entre 13 et 110 mm."))
        } else if (input.frontWheel.innerRimWidthMm >= input.frontTire.widthMm) {
            errors.add(ValidationError("frontRimWidth", "La jante avant ne peut pas être plus large que le pneu."))
        }

        // 7. Largeur interne jante arrière
        if (input.rearWheel.innerRimWidthMm <= 0.0) {
            errors.add(ValidationError("rearRimWidth", "La largeur interne de la jante arrière est requise."))
        } else if (input.rearWheel.innerRimWidthMm < 13.0 || input.rearWheel.innerRimWidthMm > 110.0) {
            errors.add(ValidationError("rearRimWidth", "La largeur interne de la jante arrière doit être comprise entre 13 et 110 mm."))
        } else if (input.rearWheel.innerRimWidthMm >= input.rearTire.widthMm) {
            errors.add(ValidationError("rearRimWidth", "La jante arrière ne peut pas être plus large que le pneu."))
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

