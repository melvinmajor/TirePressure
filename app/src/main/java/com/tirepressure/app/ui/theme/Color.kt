package com.tirepressure.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.tirepressure.app.domain.model.BikeCategory
import com.tirepressure.app.domain.model.RideStyle

// Palette inspirée du logo TirePressure : Electric Lime & Dark Titanium
val ElectricLime = Color(0xFFD4FF00) // Vert lime éclatant du logo
val ElectricLimeContainerDark = Color(0xFF263300)
val ElectricLimeDeep = Color(0xFF5A7500) // Vert lisible sur fond blanc
val ElectricLimeContainerLight = Color(0xFFEAF8B5)

// Nuances sombres (Graphite profond / Titanium)
val BackgroundDark = Color(0xFF0E1217)
val SurfaceDark = Color(0xFF161B22)
val SurfaceVariantDark = Color(0xFF212732)
val TextPrimaryDark = Color(0xFFF0F4F8)
val TextSecondaryDark = Color(0xFF8B949E)
val OutlineDark = Color(0xFF303846)

// Nuances claires (Épuré & Minimaliste)
val BackgroundLight = Color(0xFFF7F9FB)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDF1F6)
val TextPrimaryLight = Color(0xFF0E1217)
val TextSecondaryLight = Color(0xFF57606A)
val OutlineLight = Color(0xFFD0D7DE)

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

fun getCategoryColor(category: BikeCategory): Color = when (category) {
    BikeCategory.ROAD -> CategoryRoad
    BikeCategory.GRAVEL -> CategoryGravel
    BikeCategory.MTB -> CategoryMtb
    BikeCategory.COMMUTE -> CategoryCommute
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
