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

@Serializable
enum class FavoriteMode(val displayName: String, @StringRes val labelRes: Int, @StringRes val descRes: Int) {
    GLOBAL("Global", R.string.favorite_mode_global, R.string.favorite_mode_global_desc),
    PER_CATEGORY("Per Category", R.string.favorite_mode_per_category, R.string.favorite_mode_per_category_desc);
}

@Serializable
enum class AppAccentColor(
    val id: String,
    val displayName: String,
    @StringRes val labelRes: Int,
    val darkPrimaryHex: Long,
    val lightPrimaryHex: Long,
    val darkContainerHex: Long,
    val lightContainerHex: Long
) {
    ELECTRIC_LIME("lime", "Electric Lime", R.string.accent_color_lime, 0xFFD4FF00, 0xFF5A7500, 0xFF263300, 0xFFEAF8B5),
    LIGHT_BLUE("blue", "Sky Blue", R.string.accent_color_blue, 0xFF38BDF8, 0xFF0284C7, 0xFF0C4A6E, 0xFFE0F2FE),
    PASTEL_ORANGE("orange", "Pastel Orange", R.string.accent_color_orange, 0xFFFB923C, 0xFFC2410C, 0xFF431407, 0xFFFFEDD5),
    GREEN("green", "Emerald Green", R.string.accent_color_green, 0xFF34D399, 0xFF059669, 0xFF064E3B, 0xFFD1FAE5),
    PURPLE("purple", "Royal Purple", R.string.accent_color_purple, 0xFFA78BFA, 0xFF7C3AED, 0xFF2E1065, 0xFFEDE9FE),
    MONOCHROME("monochrome", "Monochrome", R.string.accent_color_monochrome, 0xFFCBD5E1, 0xFF475569, 0xFF1E293B, 0xFFE2E8F0);

    fun getPrimary(isDark: Boolean): androidx.compose.ui.graphics.Color =
        androidx.compose.ui.graphics.Color(if (isDark) darkPrimaryHex else lightPrimaryHex)

    fun getContainer(isDark: Boolean): androidx.compose.ui.graphics.Color =
        androidx.compose.ui.graphics.Color(if (isDark) darkContainerHex else lightContainerHex)

    fun getOnPrimary(isDark: Boolean): androidx.compose.ui.graphics.Color =
        when (this) {
            ELECTRIC_LIME -> if (isDark) androidx.compose.ui.graphics.Color(0xFF0E1217) else androidx.compose.ui.graphics.Color.White
            MONOCHROME -> if (isDark) androidx.compose.ui.graphics.Color(0xFF0E1217) else androidx.compose.ui.graphics.Color.White
            else -> androidx.compose.ui.graphics.Color.White
        }
}

