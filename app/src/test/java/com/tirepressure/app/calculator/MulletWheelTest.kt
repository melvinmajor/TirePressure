package com.tirepressure.app.calculator

import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class MulletWheelTest {

    private val calculator = BertoTirePressureCalculator()

    @Test
    fun testWheelDiameter32InchesSupported() {
        val diam32 = WheelDiameter.DIAMETER_686
        assertEquals(686.0, diam32.diameterMm, 0.1)
        assertEquals("32\"", diam32.getLabelForStyle(RideStyle.ENDURO_MTB))

        val mtbOptions = WheelDiameter.getOptionsForStyle(RideStyle.DOWNHILL_MTB)
        assertTrue("32-inch should be available for DH MTB", mtbOptions.contains(WheelDiameter.DIAMETER_686))
        assertTrue("29-inch should be available for DH MTB", mtbOptions.contains(WheelDiameter.DIAMETER_622))
        assertEquals(WheelDiameter.DIAMETER_559, mtbOptions.first()) // Sorted smallest to largest
        assertEquals(WheelDiameter.DIAMETER_686, mtbOptions.last())
    }

    @Test
    fun testMulletSetupCalculation() {
        // Classic Mullet Enduro Setup: 29" front with 2.5" tire, 27.5" rear with 2.4" DoubleDown tire
        val mulletInput = PressureCalculationInput(
            riderWeightKg = 78.0,
            bikeWeightKg = 15.5,
            rideStyle = RideStyle.ENDURO_MTB,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(widthMm = 63.5, casing = TireCasing.REINFORCED), // 2.5" EXO+
            rearTire = TireConfig(widthMm = 60.96, casing = TireCasing.DOUBLE),     // 2.4" DoubleDown
            frontWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_622, innerRimWidthMm = 30.0, rimType = RimType.TUBELESS_CROCHET),
            rearWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_584, innerRimWidthMm = 30.0, rimType = RimType.TUBELESS_CROCHET)
        )

        val result = calculator.calculate(mulletInput)

        // Front: 45% load on 2.5" tire with 1.10 Enduro factor
        // Rear: 55% load on 2.4" DoubleDown tire with 1.10 Enduro factor
        assertTrue("Front pressure should be valid positive value", result.front.pressureBar > 1.0)
        assertTrue("Rear pressure should be higher than front due to 55% load distribution", result.rear.pressureBar > result.front.pressureBar)
        assertFalse("Should be well within safe limits", result.hasHooklessWarning)
    }

    @Test
    fun testPrototype32InchFrontWith29RearMullet() {
        // Emerging 32" front / 29" rear DH prototype
        val prototypeInput = PressureCalculationInput(
            riderWeightKg = 82.0,
            bikeWeightKg = 16.5,
            rideStyle = RideStyle.DOWNHILL_MTB,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(widthMm = 63.5, casing = TireCasing.DOUBLE),
            rearTire = TireConfig(widthMm = 63.5, casing = TireCasing.DOUBLE),
            frontWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_686, innerRimWidthMm = 30.0, rimType = RimType.TUBELESS_CROCHET),
            rearWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_622, innerRimWidthMm = 30.0, rimType = RimType.TUBELESS_CROCHET)
        )

        val result = calculator.calculate(prototypeInput)
        assertTrue(result.front.pressureBar > 1.3)
        assertTrue(result.rear.pressureBar > result.front.pressureBar)
    }
}
