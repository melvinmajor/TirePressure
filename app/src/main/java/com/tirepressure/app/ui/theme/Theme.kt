package com.tirepressure.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = ElectricLime,
    onPrimary = Color(0xFF0E1217),
    primaryContainer = ElectricLimeContainerDark,
    onPrimaryContainer = ElectricLime,
    secondary = ElectricLime,
    onSecondary = Color(0xFF0E1217),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricLimeDeep,
    onPrimary = Color.White,
    primaryContainer = ElectricLimeContainerLight,
    onPrimaryContainer = ElectricLimeDeep,
    secondary = ElectricLimeDeep,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun TirePressureTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    accentColor: AppAccentColor = AppAccentColor.ELECTRIC_LIME,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val baseColors = if (darkTheme) DarkColorScheme else LightColorScheme
    val primaryColor = accentColor.getPrimary(darkTheme)
    val containerColor = accentColor.getContainer(darkTheme)
    val onPrimaryColor = if (accentColor == AppAccentColor.ELECTRIC_LIME && darkTheme) {
        Color(0xFF0E1217)
    } else {
        Color.White
    }

    val targetColors = baseColors.copy(
        primary = primaryColor,
        onPrimary = onPrimaryColor,
        primaryContainer = containerColor,
        onPrimaryContainer = primaryColor,
        secondary = primaryColor,
        onSecondary = onPrimaryColor
    )

    val animPrimary by animateColorAsState(targetColors.primary, tween(300), label = "primary")
    val animOnPrimary by animateColorAsState(targetColors.onPrimary, tween(300), label = "onPrimary")
    val animPrimaryContainer by animateColorAsState(targetColors.primaryContainer, tween(300), label = "primaryContainer")
    val animOnPrimaryContainer by animateColorAsState(targetColors.onPrimaryContainer, tween(300), label = "onPrimaryContainer")
    val animSecondary by animateColorAsState(targetColors.secondary, tween(300), label = "secondary")
    val animOnSecondary by animateColorAsState(targetColors.onSecondary, tween(300), label = "onSecondary")
    val animBackground by animateColorAsState(targetColors.background, tween(300), label = "background")
    val animOnBackground by animateColorAsState(targetColors.onBackground, tween(300), label = "onBackground")
    val animSurface by animateColorAsState(targetColors.surface, tween(300), label = "surface")
    val animOnSurface by animateColorAsState(targetColors.onSurface, tween(300), label = "onSurface")
    val animSurfaceVariant by animateColorAsState(targetColors.surfaceVariant, tween(300), label = "surfaceVariant")
    val animOnSurfaceVariant by animateColorAsState(targetColors.onSurfaceVariant, tween(300), label = "onSurfaceVariant")
    val animOutline by animateColorAsState(targetColors.outline, tween(300), label = "outline")
    val animError by animateColorAsState(targetColors.error, tween(300), label = "error")
    val animOnError by animateColorAsState(targetColors.onError, tween(300), label = "onError")

    val colorScheme = targetColors.copy(
        primary = animPrimary,
        onPrimary = animOnPrimary,
        primaryContainer = animPrimaryContainer,
        onPrimaryContainer = animOnPrimaryContainer,
        secondary = animSecondary,
        onSecondary = animOnSecondary,
        background = animBackground,
        onBackground = animOnBackground,
        surface = animSurface,
        onSurface = animOnSurface,
        surfaceVariant = animSurfaceVariant,
        onSurfaceVariant = animOnSurfaceVariant,
        outline = animOutline,
        error = animError,
        onError = animOnError
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
