package com.tirepressure.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Guards against the 2-partition crash where non-Bundle-compatible [Screen] objects were used
 * as SaveableStateHolder keys. Detail pane keys must always be stable, distinct Strings.
 */
class DetailPaneStateKeyTest {

    @Test
    fun `detail pane keys are distinct strings for every detail destination`() {
        val keys = listOf(
            null.detailStateKey(),
            Screen.Settings.detailStateKey(),
            Screen.AppInfo.detailStateKey(),
            Screen.BikeEdit("new").detailStateKey(),
            Screen.BikeEdit("bike-42").detailStateKey()
        )
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `detail pane keys are stable across equal screens`() {
        assertEquals(Screen.BikeEdit("abc").detailStateKey(), Screen.BikeEdit("abc").detailStateKey())
        assertEquals("settings", Screen.Settings.detailStateKey())
        assertEquals("app_info", Screen.AppInfo.detailStateKey())
        assertEquals("overview_detail_placeholder", null.detailStateKey())
        assertNotEquals(Screen.BikeEdit("a").detailStateKey(), Screen.BikeEdit("b").detailStateKey())
    }
}

