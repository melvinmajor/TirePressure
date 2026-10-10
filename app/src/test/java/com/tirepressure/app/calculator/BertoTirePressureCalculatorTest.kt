package com.tirepressure.app.calculator

import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BertoTirePressureCalculatorTest {

    private lateinit var calculator: BertoTirePressureCalculator

    @Before
    fun setUp() {
        calculator = BertoTirePressureCalculator()
    }

    /**
     * Cas de référence 1 :
     * Poids utilisateur 65kg, poids vélo 13.9kg, diamètre roues 29 pouces (622mm) avant et arrière,
     * type tubeless crochet, largeur roue 2.4 pouces (60.96mm), largeur intérieure 30mm avant et arrière,
     * tire casing standard.
     * Par temps sec :
     * - Profil XC : 20.7 psi AV / 22.0 psi AR
     * - Profil Enduro : 24.1 psi AV / 25.6 psi AR
     * - Profil Route 700c : 27.4 psi AV / 29.2 psi AR
     */
    @Test
    fun testUserReferenceCase1_XC_Enduro_Road_29Inch_65kgRider_13_9kgBike() {
        val baseInput = PressureCalculationInput(
            riderWeightKg = 65.0,
            bikeWeightKg = 13.9,
            luggageWeightKg = 0.0,
            rideStyle = RideStyle.XC_MTB,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(widthMm = 60.96, casing = TireCasing.STANDARD),
            rearTire = TireConfig(widthMm = 60.96, casing = TireCasing.STANDARD),
            frontWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_622, innerRimWidthMm = 30.0, rimType = RimType.TUBELESS_CROCHET),
            rearWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_622, innerRimWidthMm = 30.0, rimType = RimType.TUBELESS_CROCHET)
        )

        // 1. Profil XC
        val xcResult = calculator.calculate(baseInput)
        assertEquals(20.7, xcResult.front.pressurePsi, 0.1)
        assertEquals(22.0, xcResult.rear.pressurePsi, 0.1)

        // 2. Profil Enduro
        val enduroInput = baseInput.copy(rideStyle = RideStyle.ENDURO_MTB)
        val enduroResult = calculator.calculate(enduroInput)
        assertEquals(24.1, enduroResult.front.pressurePsi, 0.1)
        assertEquals(25.6, enduroResult.rear.pressurePsi, 0.1)

        // 3. Profil Route (700c = DIAMETER_622)
        val roadInput = baseInput.copy(rideStyle = RideStyle.ROAD)
        val roadResult = calculator.calculate(roadInput)
        assertEquals(27.4, roadResult.front.pressurePsi, 0.1)
        assertEquals(29.2, roadResult.rear.pressurePsi, 0.1)
    }

    /**
     * Cas de référence 2 :
     * Poids utilisateur 60.5kg, poids vélo 9.5kg, diamètre roue 650 (650c = 571mm ISO),
     * type tubeless crochet, largeur de roue 35mm, largeur intérieure 22mm, tire casing renforcé.
     * En profil route par temps sec : 48.4 psi AV / 51.5 psi AR.
     */
    @Test
    fun testUserReferenceCase2_Road_650_60_5kgRider_9_5kgBike() {
        val input = PressureCalculationInput(
            riderWeightKg = 60.5,
            bikeWeightKg = 9.5,
            luggageWeightKg = 0.0,
            rideStyle = RideStyle.ROAD,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(widthMm = 35.0, casing = TireCasing.REINFORCED),
            rearTire = TireConfig(widthMm = 35.0, casing = TireCasing.REINFORCED),
            frontWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_571, innerRimWidthMm = 22.0, rimType = RimType.TUBELESS_CROCHET),
            rearWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_571, innerRimWidthMm = 22.0, rimType = RimType.TUBELESS_CROCHET)
        )

        val result = calculator.calculate(input)
        assertEquals(48.4, result.front.pressurePsi, 0.1)
        assertEquals(51.5, result.rear.pressurePsi, 0.1)
    }

    /**
     * Cas de référence 3 :
     * Poids utilisateur 60.5kg, poids vélo 14.6kg, diamètre roue 29 pouces (622mm),
     * largeur de pneu 2.4 pouces (60.96mm), tire casing standard, largeur intérieure 30mm,
     * tubeless crochet.
     * Par temps sec en profil XC : 20.2 psi AV / 21.5 psi AR.
     */
    @Test
    fun testUserReferenceCase3_XC_29Inch_60_5kgRider_14_6kgBike() {
        val input = PressureCalculationInput(
            riderWeightKg = 60.5,
            bikeWeightKg = 14.6,
            luggageWeightKg = 0.0,
            rideStyle = RideStyle.XC_MTB,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(widthMm = 60.96, casing = TireCasing.STANDARD),
            rearTire = TireConfig(widthMm = 60.96, casing = TireCasing.STANDARD),
            frontWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_622, innerRimWidthMm = 30.0, rimType = RimType.TUBELESS_CROCHET),
            rearWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_622, innerRimWidthMm = 30.0, rimType = RimType.TUBELESS_CROCHET)
        )

        val result = calculator.calculate(input)
        assertEquals(20.2, result.front.pressurePsi, 0.1)
        assertEquals(21.5, result.rear.pressurePsi, 0.1)
    }

    @Test
    fun testHooklessSafetyLimitTriggered() {
        val input = PressureCalculationInput(
            riderWeightKg = 110.0,
            bikeWeightKg = 12.0,
            luggageWeightKg = 0.0,
            rideStyle = RideStyle.ROAD,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(widthMm = 25.0, casing = TireCasing.STANDARD),
            rearTire = TireConfig(widthMm = 25.0, casing = TireCasing.STANDARD),
            frontWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_622, innerRimWidthMm = 21.0, rimType = RimType.TUBELESS_STRAIGHT_SIDE),
            rearWheel = WheelConfig(diameter = WheelDiameter.DIAMETER_622, innerRimWidthMm = 21.0, rimType = RimType.TUBELESS_STRAIGHT_SIDE)
        )

        val result = calculator.calculate(input)
        assertTrue("Doit déclencher l'alerte de sécurité Hookless", result.hasHooklessWarning)
        assertTrue(result.rear.isHooklessExceeded)
        assertEquals(5.0, result.rear.hooklessMaxThresholdBar ?: 0.0, 0.01)
    }

    @Test
    fun testSurfaceConditionMultipliers() {
        val dryInput = PressureCalculationInput(
            riderWeightKg = 75.0,
            bikeWeightKg = 8.0,
            rideStyle = RideStyle.ROAD,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(28.0),
            rearTire = TireConfig(28.0),
            frontWheel = WheelConfig(),
            rearWheel = WheelConfig()
        )
        val wetInput = dryInput.copy(surfaceCondition = SurfaceCondition.WET)
        val snowInput = dryInput.copy(surfaceCondition = SurfaceCondition.SNOW)

        val dryResult = calculator.calculate(dryInput)
        val wetResult = calculator.calculate(wetInput)
        val snowResult = calculator.calculate(snowInput)

        // Sur sol mouillé : 90% de la pression sèche
        assertEquals(dryResult.front.pressureMbar * 0.90, wetResult.front.pressureMbar, 0.1)
        assertEquals(dryResult.rear.pressureMbar * 0.90, wetResult.rear.pressureMbar, 0.1)

        // Sur neige / sol glissant : 50% de la pression sèche
        assertEquals(dryResult.front.pressureMbar * 0.50, snowResult.front.pressureMbar, 0.1)
        assertEquals(dryResult.rear.pressureMbar * 0.50, snowResult.rear.pressureMbar, 0.1)
    }

    @Test
    fun testCasingMultipliers() {
        val baseInput = PressureCalculationInput(
            riderWeightKg = 75.0,
            bikeWeightKg = 8.0,
            rideStyle = RideStyle.ROAD,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(28.0, TireCasing.STANDARD),
            rearTire = TireConfig(28.0, TireCasing.STANDARD),
            frontWheel = WheelConfig(),
            rearWheel = WheelConfig()
        )
        val thinInput = baseInput.copy(
            frontTire = TireConfig(28.0, TireCasing.THIN),
            rearTire = TireConfig(28.0, TireCasing.THIN)
        )
        val reinfInput = baseInput.copy(
            frontTire = TireConfig(28.0, TireCasing.REINFORCED),
            rearTire = TireConfig(28.0, TireCasing.REINFORCED)
        )

        val baseRes = calculator.calculate(baseInput)
        val thinRes = calculator.calculate(thinInput)
        val reinfRes = calculator.calculate(reinfInput)

        // Carcasse fine (+2.5%) vs Renforcée (-5%)
        assertEquals(baseRes.rear.pressureMbar * 1.025, thinRes.rear.pressureMbar, 0.1)
        assertEquals(baseRes.rear.pressureMbar * 0.95, reinfRes.rear.pressureMbar, 0.1)
    }
}
