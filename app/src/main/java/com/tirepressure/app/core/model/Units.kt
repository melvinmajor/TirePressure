package com.tirepressure.app.core.model

import androidx.annotation.StringRes
import com.tirepressure.app.R
import com.tirepressure.app.core.util.UnitConverter
import kotlinx.serialization.Serializable

@Serializable
enum class PressureUnit(val symbol: String, val displayName: String, @StringRes val labelRes: Int, val decimals: Int) {
    BAR("bar", "bar", R.string.unit_bar, 2),
    PSI("psi", "psi", R.string.unit_psi, 1),
    KPA("kPa", "kPa", R.string.unit_kpa, 1);

    fun format(pressureInMbar: Double): String {
        val converted = UnitConverter.fromMbar(pressureInMbar, this)
        return String.format(java.util.Locale.US, "%.${decimals}f", converted)
    }
}

@Serializable
enum class MassUnit(val symbol: String, val displayName: String, @StringRes val labelRes: Int, val decimals: Int) {
    KG("kg", "Kilogrammes (kg)", R.string.unit_kg, 1),
    LB("lb", "Livres (lb)", R.string.unit_lb, 1);

    fun format(weightInKg: Double): String {
        val converted = UnitConverter.fromKg(weightInKg, this)
        return if (this == LB) {
            Math.round(converted).toString()
        } else {
            String.format(java.util.Locale.US, "%.${decimals}f", converted)
        }
    }
}

@Serializable
enum class AppTheme(val displayName: String, @StringRes val labelRes: Int) {
    SYSTEM("Système (automatique)", R.string.settings_theme_system),
    LIGHT("Clair / Light", R.string.settings_theme_light),
    DARK("Sombre / Dark", R.string.settings_theme_dark)
}

@Serializable
enum class AppLanguage(val code: String, val displayName: String, @StringRes val labelRes: Int) {
    SYSTEM("", "Système (automatique)", R.string.settings_lang_system),
    FRENCH("fr", "Français", R.string.settings_lang_fr),
    ENGLISH("en", "English", R.string.settings_lang_en)
}
