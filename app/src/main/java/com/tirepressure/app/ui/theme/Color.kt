package com.tirepressure.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tirepressure.app.domain.model.BikeCategory
import com.tirepressure.app.domain.model.RideStyle

// Palette inspirée du logo TirePressure : Electric Lime & Dark Titanium
val ElectricLime = Color(0xFFD4FF00) // Vert lime éclatant du logo
val ElectricLimeContainerDark = Color(0xFF263300)
val ElectricLimeDeep = Color(0xFF5A7500) // Vert lisible sur fond blanc
val ElectricLimeContainerLight = Color(0xFFEAF8B5)

// Nuances sombres (True OLED Pitch Black & Dark Slate Cards)
val BackgroundDark = Color(0xFF000000) // Pitch Black True OLED (100% pixel shutoff & deep contrast)
val SurfaceDark = Color(0xFF121318) // Subtle dark slate card base
val SurfaceVariantDark = Color(0xFF181920) // Elevated dark slate surface
val TextPrimaryDark = Color(0xFFF0F4F8)
val TextSecondaryDark = Color(0xFF8B949E)
val OutlineDark = Color(0xFF272A34)

// Nuances claires (Soft Canvas & Elevated Pure White Glass)
val BackgroundLight = Color(0xFFF4F5F8) // Soft Cool Mist / Off-White Slate canvas
val SurfaceLight = Color(0xFFFFFFFF) // Pure White
val SurfaceVariantLight = Color(0xFFF6F8FA) // Delicate elevated surface
val TextPrimaryLight = Color(0xFF0E1217)
val TextSecondaryLight = Color(0xFF57606A)
val OutlineLight = Color(0xFFE2E8F0) // Delicate 10-15% border stroke

// Alertes de sécurité (ISO/ETRTO)
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberBg = Color(0xFFFEF3C7)
val WarningAmberBgDark = Color(0xFF3B2805)
val ErrorRed = Color(0xFFEF4444)
val ErrorRedBg = Color(0xFFFEE2E2)
val ErrorRedBgDark = Color(0xFF3B1212)
val SuccessGreen = Color(0xFF22C55E)

// Couleurs distinctes par Catégorie de vélo
val CategoryRoad = Color(0xFF0284C7)      // Cyan / Bleu Vitesse (Road)
val CategoryGravel = Color(0xFFEA580C)    // Terre cuite / Orange chaleureux (Gravel)
val CategoryMtb = Color(0xFF10B981)       // Vert émeraude / Forêt dynamique (MTB)
val CategoryCommute = Color(0xFF8B5CF6)   // Violet urbain moderne (Commute)

// Variantes lumineuses pour badges sur fond sombre (Dark Mode / Widgets)
val CategoryRoadDark = Color(0xFF38BDF8)
val CategoryGravelDark = Color(0xFFFB923C)
val CategoryMtbDark = Color(0xFF34D399)
val CategoryCommuteDark = Color(0xFFA78BFA)

fun getCategoryColor(category: BikeCategory): Color = when (category) {
    BikeCategory.ROAD -> CategoryRoad
    BikeCategory.GRAVEL -> CategoryGravel
    BikeCategory.MTB -> CategoryMtb
    BikeCategory.COMMUTE -> CategoryCommute
}

fun getCategoryColorInt(category: BikeCategory, isDark: Boolean): Int = when (category) {
    BikeCategory.ROAD -> (if (isDark) CategoryRoadDark else CategoryRoad).toArgb()
    BikeCategory.GRAVEL -> (if (isDark) CategoryGravelDark else CategoryGravel).toArgb()
    BikeCategory.MTB -> (if (isDark) CategoryMtbDark else CategoryMtb).toArgb()
    BikeCategory.COMMUTE -> (if (isDark) CategoryCommuteDark else CategoryCommute).toArgb()
}

fun getRideStyleColor(style: RideStyle, fallbackCategory: BikeCategory): Color {
    val matchedCat = when (style) {
        RideStyle.ROAD -> BikeCategory.ROAD
        RideStyle.GRAVEL, RideStyle.CYCLOCROSS -> BikeCategory.GRAVEL
        RideStyle.XC_MTB, RideStyle.TRAIL_MTB, RideStyle.ENDURO_MTB, RideStyle.DOWNHILL_MTB, RideStyle.FAT_BIKE -> BikeCategory.MTB
        RideStyle.COMMUTE -> BikeCategory.COMMUTE
    }
    return getCategoryColor(matchedCat)
}

fun getRideStyleColorInt(style: RideStyle, fallbackCategory: BikeCategory, isDark: Boolean): Int {
    val matchedCat = when (style) {
        RideStyle.ROAD -> BikeCategory.ROAD
        RideStyle.GRAVEL, RideStyle.CYCLOCROSS -> BikeCategory.GRAVEL
        RideStyle.XC_MTB, RideStyle.TRAIL_MTB, RideStyle.ENDURO_MTB, RideStyle.DOWNHILL_MTB, RideStyle.FAT_BIKE -> BikeCategory.MTB
        RideStyle.COMMUTE -> BikeCategory.COMMUTE
    }
    return getCategoryColorInt(matchedCat, isDark)
}
