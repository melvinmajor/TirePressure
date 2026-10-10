package com.tirepressure.app.calculator

import com.tirepressure.app.domain.calculator.TirePressureValidator
import com.tirepressure.app.domain.model.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TirePressureValidatorTest {

    @Test
    fun testValidConfigurationPasses() {
        val input = PressureCalculationInput(
            riderWeightKg = 75.0,
            bikeWeightKg = 8.0,
            luggageWeightKg = 2.0,
            rideStyle = RideStyle.ROAD,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(28.0),
            rearTire = TireConfig(28.0),
            frontWheel = WheelConfig(innerRimWidthMm = 21.0),
            rearWheel = WheelConfig(innerRimWidthMm = 21.0)
        )

        val result = TirePressureValidator.validate(input)
        assertTrue("La configuration valide doit être acceptée", result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun testNegativeOrZeroWeightsRejected() {
        val input = PressureCalculationInput(
            riderWeightKg = 0.0,
            bikeWeightKg = -5.0,
            luggageWeightKg = -1.0,
            rideStyle = RideStyle.ROAD,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(28.0),
            rearTire = TireConfig(28.0),
            frontWheel = WheelConfig(innerRimWidthMm = 21.0),
            rearWheel = WheelConfig(innerRimWidthMm = 21.0)
        )

        val result = TirePressureValidator.validate(input)
        assertFalse("Les poids négatifs ou nuls doivent être rejetés", result.isValid)
        assertTrue(result.errors.any { it.field == "riderWeight" })
        assertTrue(result.errors.any { it.field == "bikeWeight" })
        assertTrue(result.errors.any { it.field == "luggageWeight" })
    }

    @Test
    fun testRimWiderThanTireRejected() {
        // Une jante de 30mm avec un pneu de 25mm est dangereuse et physiquement impossible
        val input = PressureCalculationInput(
            riderWeightKg = 75.0,
            bikeWeightKg = 8.0,
            rideStyle = RideStyle.ROAD,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(25.0),
            rearTire = TireConfig(25.0),
            frontWheel = WheelConfig(innerRimWidthMm = 30.0),
            rearWheel = WheelConfig(innerRimWidthMm = 30.0)
        )

        val result = TirePressureValidator.validate(input)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "frontRimWidth" })
        assertTrue(result.errors.any { it.field == "rearRimWidth" })
    }
}

