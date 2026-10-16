package com.tirepressure.app.widget

import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.PressureCalculationInput
import com.tirepressure.app.domain.model.RideStyle
import com.tirepressure.app.domain.model.SurfaceCondition
import com.tirepressure.app.domain.model.TireConfig
import com.tirepressure.app.domain.model.WheelConfig
import com.tirepressure.app.domain.model.WheelPressureResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetTest {

    @Test
    fun testFormatPressure() {
        val wheelResult = WheelPressureResult(
            pressureMbar = 2200.0,
            pressureBar = 2.2,
            pressurePsi = 31.9,
            pressureKpa = 220.0,
            isHooklessExceeded = false,
            hooklessMaxThresholdBar = null,
            isIncompatibleRimTire = false
        )

        val barStr = SingleBikeWidgetReceiver.formatPressure(wheelResult, PressureUnit.BAR)
        val psiStr = SingleBikeWidgetReceiver.formatPressure(wheelResult, PressureUnit.PSI)
        val kpaStr = SingleBikeWidgetReceiver.formatPressure(wheelResult, PressureUnit.KPA)

        assertEquals("2.2 bar", barStr)
        assertEquals("32 psi", psiStr)
        assertEquals("220 kPa", kpaStr)
    }

    @Test
    fun testWidgetThemeEnum() {
        val themes = WidgetTheme.values()
        assertEquals(3, themes.size)
        assertTrue(themes.contains(WidgetTheme.SYSTEM))
        assertTrue(themes.contains(WidgetTheme.LIGHT))
        assertTrue(themes.contains(WidgetTheme.DARK))
    }

    @Test
    fun testWidgetCalculationWithDiscipline() {
        val calc = BertoTirePressureCalculator()
        val input = PressureCalculationInput(
            riderWeightKg = 75.0,
            bikeWeightKg = 9.0,
            luggageWeightKg = 0.0,
            rideStyle = RideStyle.GRAVEL,
            surfaceCondition = SurfaceCondition.DRY,
            frontTire = TireConfig(40.0),
            rearTire = TireConfig(40.0),
            frontWheel = WheelConfig(),
            rearWheel = WheelConfig()
        )

        val result = calc.calculate(input)
        assertNotNull(result)
        assertTrue(result.front.pressureBar > 1.5 && result.front.pressureBar < 3.5)
        assertTrue(result.rear.pressureBar > result.front.pressureBar)

        val frontBar = SingleBikeWidgetReceiver.formatPressure(result.front, PressureUnit.BAR)
        val rearBar = SingleBikeWidgetReceiver.formatPressure(result.rear, PressureUnit.BAR)
        assertTrue(frontBar.endsWith("bar"))
        assertTrue(rearBar.endsWith("bar"))
    }
}
