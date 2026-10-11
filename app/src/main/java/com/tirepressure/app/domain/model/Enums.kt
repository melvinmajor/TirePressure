package com.tirepressure.app.domain.model

import androidx.annotation.StringRes
import com.tirepressure.app.R
import kotlinx.serialization.Serializable

@Serializable
enum class RideStyle(
    val displayName: String,
    @StringRes val labelRes: Int,
    val defaultBikeWeightKg: Double,
    val defaultInnerRimWidthMm: Double,
    val defaultRimType: RimType,
    val defaultTireWidthMm: Double,
    val defaultTireCasing: TireCasing,
    val isMtbInches: Boolean
) {
    ROAD("Route", R.string.style_road, 7.5, 21.0, RimType.TUBELESS_STRAIGHT_SIDE, 28.0, TireCasing.STANDARD, false),
    GRAVEL("Gravel", R.string.style_gravel, 9.0, 25.0, RimType.TUBELESS_STRAIGHT_SIDE, 40.0, TireCasing.STANDARD, false),
    CYCLOCROSS("Cyclocross", R.string.style_cross, 8.0, 21.0, RimType.TUBULAR, 33.0, TireCasing.STANDARD, false),
    XC_MTB("VTT Cross-Country", R.string.style_xc_mtb, 10.5, 30.0, RimType.TUBELESS_STRAIGHT_SIDE, 58.42, TireCasing.STANDARD, true),
    TRAIL_MTB("VTT Trail", R.string.style_trail_mtb, 13.0, 30.0, RimType.TUBELESS_STRAIGHT_SIDE, 60.96, TireCasing.REINFORCED, true),
    ENDURO_MTB("VTT Enduro", R.string.style_enduro_mtb, 14.5, 30.0, RimType.TUBELESS_STRAIGHT_SIDE, 60.96, TireCasing.REINFORCED, true),
    DOWNHILL_MTB("VTT Descente", R.string.style_dh_mtb, 16.0, 30.0, RimType.TUBELESS_CROCHET, 63.5, TireCasing.DOUBLE, true),
    FAT_BIKE("Fat Bike", R.string.style_fat, 15.0, 80.0, RimType.TUBELESS_CROCHET, 96.52, TireCasing.STANDARD, true),
    COMMUTE("Vélotaf / Urbain", R.string.style_commute, 14.0, 21.0, RimType.TUBES, 38.0, TireCasing.STANDARD, false);
}

/**
 * Catégories principales de vélos (sélectionnables à la création ou modification du vélo).
 * Le calcul précis de la pression des pneus est effectué au niveau des profils (configurations)
 * via les disciplines détaillées (RideStyle).
 */
@Serializable
enum class BikeCategory(
    val displayName: String,
    @StringRes val labelRes: Int,
    val defaultWeightKg: Double,
    val defaultRideStyle: RideStyle
) {
    ROAD("Route", R.string.category_road, 8.0, RideStyle.ROAD),
    GRAVEL("Gravel", R.string.category_gravel, 9.5, RideStyle.GRAVEL),
    MTB("VTT", R.string.category_mtb, 13.0, RideStyle.XC_MTB),
    COMMUTE("Vélotaf / Urbain", R.string.category_commute, 14.0, RideStyle.COMMUTE);
}

@Serializable
enum class RimType(val displayName: String, @StringRes val labelRes: Int, val isHookless: Boolean) {
    TUBELESS_STRAIGHT_SIDE("Tubeless Hookless (sans crochets)", R.string.rim_tubeless_straight, true),
    TUBELESS_CROCHET("Tubeless avec crochets", R.string.rim_tubeless_crochet, false),
    TUBES("Chambre à air (Clincher)", R.string.rim_tubes, false),
    TUBULAR("Boyau (Tubular)", R.string.rim_tubular, false),
    WIDE_AERO_HOOKLESS("Wide Aero Hookless (32mm+)", R.string.rim_zipp_303_xplr, true);
}

@Serializable
enum class TireCasing(val displayName: String, @StringRes val labelRes: Int) {
    STANDARD("Standard", R.string.casing_standard),
    THIN("Fine / Légère (Supple / High TPI)", R.string.casing_thin),
    REINFORCED("Renforcée (EXO, Grid, etc.)", R.string.casing_reinforced),
    DOUBLE("Double carcasse (DH, DoubleDown)", R.string.casing_double);
}

@Serializable
enum class SurfaceCondition(val displayName: String, @StringRes val labelRes: Int) {
    DRY("Sec (Dry)", R.string.overview_dry),
    WET("Pluie (Rain)", R.string.overview_wet),
    SNOW("Neige (Snow)", R.string.overview_snow);
}

@Serializable
enum class WheelDiameter(val diameterMm: Double, val defaultLabel: String) {
    DIAMETER_559(559.0, "26\""),
    DIAMETER_571(571.0, "650c"),
    DIAMETER_584(584.0, "650b / 27.5\""),
    DIAMETER_622(622.0, "700c / 29\""),
    DIAMETER_686(686.0, "32\"");

    fun getLabelForStyle(style: RideStyle): String {
        return when (style) {
            RideStyle.XC_MTB, RideStyle.TRAIL_MTB, RideStyle.ENDURO_MTB, RideStyle.DOWNHILL_MTB -> when (this) {
                DIAMETER_559 -> "26\""
                DIAMETER_571 -> "26\""
                DIAMETER_584 -> "27.5\""
                DIAMETER_622 -> "29\""
                DIAMETER_686 -> "32\""
            }
            RideStyle.ROAD, RideStyle.GRAVEL, RideStyle.CYCLOCROSS, RideStyle.COMMUTE -> when (this) {
                DIAMETER_559 -> "26\""
                DIAMETER_571 -> "650c"
                DIAMETER_584 -> "650b"
                DIAMETER_622 -> "700c"
                DIAMETER_686 -> "32\""
            }
            RideStyle.FAT_BIKE -> when (this) {
                DIAMETER_559 -> "26\""
                DIAMETER_584 -> "27.5\""
                else -> defaultLabel
            }
        }
    }

    companion object {
        fun getOptionsForStyle(style: RideStyle): List<WheelDiameter> {
            val list = when (style) {
                RideStyle.ROAD -> listOf(DIAMETER_571, DIAMETER_584, DIAMETER_622)
                RideStyle.GRAVEL -> listOf(DIAMETER_584, DIAMETER_622)
                RideStyle.CYCLOCROSS -> listOf(DIAMETER_622)
                RideStyle.COMMUTE -> listOf(DIAMETER_559, DIAMETER_584, DIAMETER_622)
                RideStyle.XC_MTB, RideStyle.TRAIL_MTB, RideStyle.ENDURO_MTB, RideStyle.DOWNHILL_MTB -> listOf(DIAMETER_559, DIAMETER_584, DIAMETER_622, DIAMETER_686)
                RideStyle.FAT_BIKE -> listOf(DIAMETER_559, DIAMETER_584)
            }
            return list.sortedBy { it.diameterMm }
        }
    }
}
