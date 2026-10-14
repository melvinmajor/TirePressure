package com.tirepressure.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tirepressure.app.R

// -----------------------------------------------------------------------------
// Hubot Sans: Brand expression, titles, headings, and prominent pressure displays
// -----------------------------------------------------------------------------
val HubotSans = FontFamily(
    Font(R.font.hubot_sans_regular, FontWeight.Normal),
    Font(R.font.hubot_sans_medium, FontWeight.Medium),
    Font(R.font.hubot_sans_semibold, FontWeight.SemiBold),
    Font(R.font.hubot_sans_bold, FontWeight.Bold),
    Font(R.font.hubot_sans_extrabold, FontWeight.ExtraBold)
)

// -----------------------------------------------------------------------------
// Inter: Body text, form fields, labels, settings, dialogs, and UI elements
// -----------------------------------------------------------------------------
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

// -----------------------------------------------------------------------------
// Centralized Material 3 Typography System
// -----------------------------------------------------------------------------
val Typography = Typography(
    // Prominent Numeric & Brand Displays (Hubot Sans)
    displayLarge = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),

    // Screen and Page Titles (Hubot Sans)
    headlineLarge = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),

    // Section Headings & Emphasized Cards (Hubot Sans)
    titleLarge = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    // Body Text, Descriptions & Explanatory Copy (Inter)
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),

    // Form Labels, Buttons, Chips & Ordinary Interface Text (Inter)
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

// -----------------------------------------------------------------------------
// Centralized Domain-Specific Typography Extensions
// -----------------------------------------------------------------------------
/**
 * Prominent pressure reading style (e.g. "2.4 bar" or "32 psi").
 * Consumes Hubot Sans for high-visibility numeric and brand emphasis.
 */
val Typography.pressureReading: TextStyle
    get() = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )

/**
 * Hero pressure reading style for large single-number emphasis.
 */
val Typography.pressureHero: TextStyle
    get() = TextStyle(
        fontFamily = HubotSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    )
