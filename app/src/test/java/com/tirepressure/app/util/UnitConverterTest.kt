package com.tirepressure.app.util

import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.util.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun testPressureConversionsExactness() {
        // 1 bar = 1000 mbar = 100 kPa
        val mbar1000 = 1000.0
        assertEquals(1.0, UnitConverter.fromMbar(mbar1000, PressureUnit.BAR), 0.0001)
        assertEquals(100.0, UnitConverter.fromMbar(mbar1000, PressureUnit.KPA), 0.0001)
        assertEquals(14.5038, UnitConverter.fromMbar(mbar1000, PressureUnit.PSI), 0.001)

        // Aller-retour
        val psi50 = 50.0
        val mbarFromPsi = UnitConverter.toMbar(psi50, PressureUnit.PSI)
        val backToPsi = UnitConverter.fromMbar(mbarFromPsi, PressureUnit.PSI)
        assertEquals(50.0, backToPsi, 0.0001)

        val bar3 = 3.0
        val mbarFromBar = UnitConverter.toMbar(bar3, PressureUnit.BAR)
        assertEquals(3000.0, mbarFromBar, 0.0001)
    }

    @Test
    fun testMassConversions() {
        val kg75 = 75.0
        val lbs = UnitConverter.fromKg(kg75, MassUnit.LB)
        assertEquals(165.3467, lbs, 0.01)

        val backToKg = UnitConverter.toKg(lbs, MassUnit.LB)
        assertEquals(75.0, backToKg, 0.0001)
    }

    @Test
    fun testRoundingRules() {
        // Bar : 2 décimales
        assertEquals("4.05", PressureUnit.BAR.format(4053.7))
        assertEquals("2.21", PressureUnit.BAR.format(2212.0))

        // PSI : 1 décimale
        assertEquals("58.8", PressureUnit.PSI.format(4053.7))
        assertEquals("32.1", PressureUnit.PSI.format(2212.0))

        // kPa : 1 décimale
        assertEquals("405.4", PressureUnit.KPA.format(4053.7))

        // Masse kg : 1 décimale
        assertEquals("75.0", MassUnit.KG.format(75.0))
        // Masse lb : entier arrondi
        assertEquals("165", MassUnit.LB.format(75.0))
    }
}

