package com.tirepressure.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import io.github.angelomarzocchi.lumen.rememberBounceHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.tirepressure.app.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.tirepressure.app.ui.theme.WarningAmber
import com.tirepressure.app.ui.theme.WarningAmberBg
import com.tirepressure.app.ui.theme.WarningAmberBgDark

import io.github.angelomarzocchi.glass.GlassBackdrop
import io.github.angelomarzocchi.glass.GlassMaterial
import io.github.angelomarzocchi.glass.glassLampFollowsFinger
import io.github.angelomarzocchi.glass.glassPosition
import io.github.angelomarzocchi.glass.rememberGlassLamp
import io.github.angelomarzocchi.glass.rememberGlassPainter
import io.github.angelomarzocchi.glass.rememberTiltKeyLight
import io.github.angelomarzocchi.lumen.GlossCardColors
import io.github.angelomarzocchi.lumen.GlossCardDefaults
import io.github.angelomarzocchi.lumen.LightTab
import io.github.angelomarzocchi.lumen.LightTabs
import io.github.angelomarzocchi.lumen.LumenDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tirepressure.app.domain.model.BikeCategory

import androidx.compose.ui.window.DialogProperties

/**
 * Utility to format decimal numbers cleanly up to maxDecimals places without trailing zeros.
 * e.g., 2.35 -> "2.35", 2.0 -> "2", 59.69 -> "59.69", 60.0 -> "60"
 */
fun formatDecimal(value: Double, maxDecimals: Int = 2): String {
    if (value <= 0.0) return ""
    val factor = when (maxDecimals) {
        1 -> 10.0
        2 -> 100.0
        else -> 100.0
    }
    val rounded = Math.round(value * factor) / factor
    return if (Math.abs(rounded - Math.round(rounded)) < 0.0001) {
        rounded.toLong().toString()
    } else {
        val str = String.format(java.util.Locale.US, "%.${maxDecimals}f", rounded)
        str.trimEnd('0').trimEnd('.')
    }
}

/**
 * Shared adaptive colors provider for GlossCard components.
 * Seamlessly adapts surface, highlight, slab border, and shadow between Dark and Light themes.
 */
@Composable
fun appGlossCardColors(
    base: Color = MaterialTheme.colorScheme.surface,
    content: Color = MaterialTheme.colorScheme.onSurface,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
): GlossCardColors {
    return if (isDark) {
        GlossCardColors(
            top = Color(0xFF181920),
            middle = base,
            bottom = Color(0xFF121318),
            slab = Color.White.copy(alpha = 0.08f),
            content = content,
            shadow = Color.Black.copy(alpha = 0.60f),
            contactShadow = Color.Black.copy(alpha = 0.30f)
        )
    } else {
        GlossCardColors(
            top = Color.White,
            middle = Color.White,
            bottom = Color.White,
            slab = Color(0xFFE2E8F0),
            content = content,
            shadow = Color(0xFF0F172A).copy(alpha = 0.08f),
            contactShadow = Color(0xFF0F172A).copy(alpha = 0.04f)
        )
    }
}

/**
 * Reusable Glassmorphic 3D Card with translucent gradient surface, specular highlight border,
 * and soft ambient/spot depth shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: androidx.compose.ui.unit.Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 3D volume shadow with enhanced ambient & spot diffusion - softened in light mode
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.60f) else Color(0xFF0F172A).copy(alpha = 0.08f)
    val ambientColor = if (isDark) Color.Black.copy(alpha = 0.30f) else Color(0xFF0F172A).copy(alpha = 0.04f)

    // Frosted glass gradient background with subtle dark slate cards (#121318 to #181920) or pure white in light mode
    val glassBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF181920).copy(alpha = 0.85f),
                Color(0xFF121318).copy(alpha = 0.80f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.90f)
            )
        )
    }

    // Frosted specular rim border gradient (#E2E8F0 in light theme, soft white rim in dark theme)
    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE2E8F0),
                Color(0xFFE2E8F0).copy(alpha = 0.85f),
                Color(0xFFE2E8F0).copy(alpha = 0.60f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = ambientColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(glassBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
    ) {
        content()
    }
}

val LocalGlassBackdrop = androidx.compose.runtime.compositionLocalOf<GlassBackdrop?> { null }

/**
 * Reusable Frosted Glass Alert Dialog with translucent surface, specular rim border,
 * soft ambient/spot depth shadow, and 24.dp rounded corners.
 * Powered by compose-glass AGSL frosted material when LocalGlassBackdrop is available.
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    properties: DialogProperties = DialogProperties()
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = LocalGlassBackdrop.current
    val painter = rememberGlassPainter()
    val glassLamp = rememberGlassLamp()
    val roomLight = rememberTiltKeyLight()

    // Window blur-behind for Android 12+ (API 31+) to blur content beneath the dialog
    val view = LocalView.current
    DisposableEffect(view) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            var parent = view.parent
            while (parent != null) {
                if (parent is DialogWindowProvider) {
                    val window = parent.window
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes.blurBehindRadius = 48
                    break
                }
                parent = parent.parent
            }
        }
        onDispose { }
    }

    val glassBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF181920).copy(alpha = 0.90f),
                Color(0xFF121318).copy(alpha = 0.95f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                Color.White.copy(alpha = 0.92f)
            )
        )
    }

    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE2E8F0),
                Color(0xFFE2E8F0).copy(alpha = 0.85f),
                Color(0xFFE2E8F0).copy(alpha = 0.60f)
            )
        )
    }

    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color(0xFF475569).copy(alpha = 0.22f)

    val dialogModifier = if (backdrop != null) {
        modifier
            .shadow(24.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .glassPosition(painter)
            .drawBehind {
                val radius = 24.dp.toPx()
                painter.light {
                    lamp = androidx.compose.ui.geometry.Offset(glassLamp.x.value, glassLamp.y.value)
                    lampOn = glassLamp.on.value
                    keyLight = roomLight()
                }
                with(painter) {
                    this@drawBehind.drawBody(
                        backdrop = backdrop,
                        material = GlassMaterial.frosted(isDark),
                        shape = androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, this@drawBehind.size),
                        radius = radius,
                        dark = isDark
                    )
                }
            }
            .glassLampFollowsFinger(glassLamp)
            .border(BorderStroke(1.dp, borderBrush), shape)
    } else {
        modifier
            .shadow(24.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .background(glassBrush, shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        properties = properties,
        modifier = dialogModifier
    )
}

/**
 * Authentic Topographic Contour Lines Header pattern for MTB (Mountain Bike) cards.
 * Features concentric mountain summits, saddle passes, and elevation isolines on a flat background.
 */
@Composable
fun BikeCardTopoHeader(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val intermediateColor = if (isDark) Color.White.copy(alpha = 0.055f) else Color(0xFF334155).copy(alpha = 0.075f)
    val indexColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF1E293B).copy(alpha = 0.13f)
    val flatBgColor = if (isDark) Color.White.copy(alpha = 0.018f) else Color(0xFF0F172A).copy(alpha = 0.022f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
    ) {
        val w = size.width
        val h = size.height

        // 1. Flat background fill
        drawRect(color = flatBgColor)

        val thinStroke = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val indexStroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // 2. West Mountain Peak (Summit A at x=0.28w, y=0.48h)
        val cxA = 0.28f * w
        val cyA = 0.48f * h

        // Peak A summit mark (+)
        val tick = 2.0.dp.toPx()
        drawLine(indexColor, androidx.compose.ui.geometry.Offset(cxA - tick, cyA), androidx.compose.ui.geometry.Offset(cxA + tick, cyA), strokeWidth = 1.2.dp.toPx())
        drawLine(indexColor, androidx.compose.ui.geometry.Offset(cxA, cyA - tick), androidx.compose.ui.geometry.Offset(cxA, cyA + tick), strokeWidth = 1.2.dp.toPx())

        // Peak A contour loops (Level 3 summit cap)
        drawPath(
            path = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(cxA - 14.dp.toPx(), cyA - 4.5.dp.toPx(), cxA + 14.dp.toPx(), cyA + 4.5.dp.toPx()))
            },
            color = intermediateColor,
            style = thinStroke
        )

        // Peak A contour loop (Level 2 index loop)
        drawPath(
            path = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(cxA - 28.dp.toPx(), cyA - 8.5.dp.toPx(), cxA + 28.dp.toPx(), cyA + 8.5.dp.toPx()))
            },
            color = indexColor,
            style = indexStroke
        )

        // Peak A contour loop (Level 1 outer loop)
        drawPath(
            path = Path().apply {
                moveTo(cxA - 42.dp.toPx(), cyA)
                cubicTo(cxA - 42.dp.toPx(), cyA - 13.dp.toPx(), cxA + 40.dp.toPx(), cyA - 13.dp.toPx(), cxA + 42.dp.toPx(), cyA)
                cubicTo(cxA + 44.dp.toPx(), cyA + 13.dp.toPx(), cxA - 40.dp.toPx(), cyA + 13.dp.toPx(), cxA - 42.dp.toPx(), cyA)
                close()
            },
            color = intermediateColor,
            style = thinStroke
        )

        // 3. East Mountain Peak (Summit B at x=0.74w, y=0.44h)
        val cxB = 0.74f * w
        val cyB = 0.44f * h

        // Peak B summit mark (+)
        drawLine(indexColor, androidx.compose.ui.geometry.Offset(cxB - tick, cyB), androidx.compose.ui.geometry.Offset(cxB + tick, cyB), strokeWidth = 1.2.dp.toPx())
        drawLine(indexColor, androidx.compose.ui.geometry.Offset(cxB, cyB - tick), androidx.compose.ui.geometry.Offset(cxB, cyB + tick), strokeWidth = 1.2.dp.toPx())

        // Peak B contour loops (Level 3 summit cap)
        drawPath(
            path = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(cxB - 18.dp.toPx(), cyB - 5.dp.toPx(), cxB + 18.dp.toPx(), cyB + 5.dp.toPx()))
            },
            color = intermediateColor,
            style = thinStroke
        )

        // Peak B contour loop (Level 2 index loop)
        drawPath(
            path = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(cxB - 34.dp.toPx(), cyB - 9.5.dp.toPx(), cxB + 34.dp.toPx(), cyB + 9.5.dp.toPx()))
            },
            color = indexColor,
            style = indexStroke
        )

        // Peak B contour loop (Level 1 outer loop)
        drawPath(
            path = Path().apply {
                moveTo(cxB - 50.dp.toPx(), cyB)
                cubicTo(cxB - 50.dp.toPx(), cyB - 14.dp.toPx(), cxB + 48.dp.toPx(), cyB - 14.dp.toPx(), cxB + 50.dp.toPx(), cyB)
                cubicTo(cxB + 52.dp.toPx(), cyB + 14.dp.toPx(), cxB - 48.dp.toPx(), cyB + 14.dp.toPx(), cxB - 50.dp.toPx(), cyB)
                close()
            },
            color = intermediateColor,
            style = thinStroke
        )

        // 4. Saddle / Mountain Col between the peaks (hyperbolic contour pass)
        val saddleP = Path().apply {
            moveTo(0.48f * w, 0f)
            cubicTo(0.49f * w, 0.32f * h, 0.52f * w, 0.38f * h, 0.52f * w, 0.50f * h)
            cubicTo(0.52f * w, 0.62f * h, 0.49f * w, 0.68f * h, 0.48f * w, h)
        }
        drawPath(saddleP, color = intermediateColor, style = thinStroke)

        val saddleP2 = Path().apply {
            moveTo(0.55f * w, 0f)
            cubicTo(0.54f * w, 0.30f * h, 0.52f * w, 0.42f * h, 0.52f * w, 0.50f * h)
            cubicTo(0.52f * w, 0.58f * h, 0.54f * w, 0.70f * h, 0.55f * w, h)
        }
        drawPath(saddleP2, color = intermediateColor, style = thinStroke)

        // 5. Alpine Valley Contours embracing the mountain massif
        val northSlope = Path().apply {
            moveTo(0f, 0.15f * h)
            cubicTo(0.12f * w, 0.08f * h, 0.28f * w, 0.18f * h, 0.42f * w, 0.10f * h)
            cubicTo(0.58f * w, 0.02f * h, 0.74f * w, 0.16f * h, w, 0.08f * h)
        }
        drawPath(northSlope, color = indexColor, style = indexStroke)

        val southSlope = Path().apply {
            moveTo(0f, 0.85f * h)
            cubicTo(0.15f * w, 0.90f * h, 0.28f * w, 0.78f * h, 0.44f * w, 0.88f * h)
            cubicTo(0.60f * w, 0.98f * h, 0.74f * w, 0.82f * h, w, 0.90f * h)
        }
        drawPath(southSlope, color = indexColor, style = indexStroke)
    }
}

/**
 * Aerodynamic Hazard / Speed Chevron Stripes Header pattern for Road bikes.
 * Features crisp diagonal speed stripes on a flat background.
 */
@Composable
fun BikeCardHazardHeader(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val stripeColor = if (isDark) Color.White.copy(alpha = 0.055f) else Color(0xFF0F172A).copy(alpha = 0.065f)
    val indexStripeColor = if (isDark) Color.White.copy(alpha = 0.095f) else Color(0xFF0F172A).copy(alpha = 0.11f)
    val flatBgColor = if (isDark) Color.White.copy(alpha = 0.018f) else Color(0xFF0F172A).copy(alpha = 0.022f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
    ) {
        val w = size.width
        val h = size.height

        drawRect(color = flatBgColor)

        val stepPx = 22.dp.toPx()
        val stripeWidth = 2.5.dp.toPx()
        val indexStripeWidth = 3.5.dp.toPx()
        val tanAngle = 0.70f // ~35 degree slant
        val deltaX = h * tanAngle

        var index = 0
        var currentX = -deltaX - (stepPx * 2)
        while (currentX < w + deltaX + stepPx) {
            val isIndex = index % 4 == 0
            drawLine(
                color = if (isIndex) indexStripeColor else stripeColor,
                start = androidx.compose.ui.geometry.Offset(currentX, 0f),
                end = androidx.compose.ui.geometry.Offset(currentX + deltaX, h),
                strokeWidth = if (isIndex) indexStripeWidth else stripeWidth,
                cap = StrokeCap.Square
            )
            currentX += stepPx
            index++
        }
    }
}

/**
 * Diamond Micro-Tread & Knurl Matrix Header pattern for Gravel / All-Road bikes.
 * Inspired by classic all-road & gravel tires (e.g. GravelKing SK, G-One Allround).
 * Features an interlocking isometric diamond knurl lattice with centered micro-studs.
 */
@Composable
fun BikeCardGravelHeader(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val treadColor = if (isDark) Color.White.copy(alpha = 0.065f) else Color(0xFF334155).copy(alpha = 0.085f)
    val indexTreadColor = if (isDark) Color.White.copy(alpha = 0.11f) else Color(0xFF0F172A).copy(alpha = 0.13f)
    val studColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF1E293B).copy(alpha = 0.14f)
    val flatBgColor = if (isDark) Color.White.copy(alpha = 0.018f) else Color(0xFF0F172A).copy(alpha = 0.022f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
    ) {
        val w = size.width
        val h = size.height

        drawRect(color = flatBgColor)

        // Isometric diamond micro-tread matrix
        val stepPx = 14.dp.toPx()
        val tanAngle = 0.85f
        val deltaX = h * tanAngle

        // Family 1: Positive diagonal slashes (/)
        var index = 0
        var x = -deltaX - (stepPx * 2)
        while (x < w + deltaX + stepPx) {
            val isIndex = index % 4 == 0
            drawLine(
                color = if (isIndex) indexTreadColor else treadColor,
                start = androidx.compose.ui.geometry.Offset(x, h),
                end = androidx.compose.ui.geometry.Offset(x + deltaX, 0f),
                strokeWidth = if (isIndex) 1.4.dp.toPx() else 1.0.dp.toPx(),
                cap = StrokeCap.Round
            )
            x += stepPx
            index++
        }

        // Family 2: Negative diagonal slashes (\)
        index = 0
        x = -deltaX - (stepPx * 2)
        while (x < w + deltaX + stepPx) {
            val isIndex = index % 4 == 0
            drawLine(
                color = if (isIndex) indexTreadColor else treadColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x + deltaX, h),
                strokeWidth = if (isIndex) 1.4.dp.toPx() else 1.0.dp.toPx(),
                cap = StrokeCap.Round
            )
            x += stepPx
            index++
        }

        // Center micro-studs at the heart of diamond cells
        val rowHeight = stepPx / (2 * tanAngle)
        val numRows = (h / rowHeight).toInt() + 1
        for (r in 0..numRows) {
            val cy = r * rowHeight
            val xOffset = if (r % 2 == 0) 0f else stepPx / 2f
            var cx = xOffset
            while (cx < w) {
                drawCircle(
                    color = studColor,
                    radius = 1.2.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )
                cx += stepPx
            }
        }
    }
}

/**
 * Urban Transit Grid & Metro Lines Header pattern for Commute / City bikes.
 * Features arterial avenues, 45° transit connections, and station interchange nodes.
 */
@Composable
fun BikeCardCommuteHeader(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val transitLineColor = if (isDark) Color.White.copy(alpha = 0.065f) else Color(0xFF334155).copy(alpha = 0.085f)
    val indexLineColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF1E293B).copy(alpha = 0.13f)
    val stationColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFF0F172A).copy(alpha = 0.16f)
    val flatBgColor = if (isDark) Color.White.copy(alpha = 0.018f) else Color(0xFF0F172A).copy(alpha = 0.022f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
    ) {
        val w = size.width
        val h = size.height

        drawRect(color = flatBgColor)

        val thinStroke = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round)
        val mainStroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)

        // 1. Horizontal arterial lines
        drawLine(
            color = indexLineColor,
            start = androidx.compose.ui.geometry.Offset(0f, 0.50f * h),
            end = androidx.compose.ui.geometry.Offset(w, 0.50f * h),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = transitLineColor,
            start = androidx.compose.ui.geometry.Offset(0f, 0.22f * h),
            end = androidx.compose.ui.geometry.Offset(w, 0.22f * h),
            strokeWidth = 1.0.dp.toPx()
        )
        drawLine(
            color = transitLineColor,
            start = androidx.compose.ui.geometry.Offset(0f, 0.78f * h),
            end = androidx.compose.ui.geometry.Offset(w, 0.78f * h),
            strokeWidth = 1.0.dp.toPx()
        )

        // 2. 45-degree diagonal transit routes
        val d1 = Path().apply {
            moveTo(0.12f * w, 0.22f * h)
            lineTo(0.24f * w, 0.50f * h)
            lineTo(0.36f * w, 0.78f * h)
        }
        drawPath(d1, color = transitLineColor, style = thinStroke)

        val d2 = Path().apply {
            moveTo(0.58f * w, 0.78f * h)
            lineTo(0.70f * w, 0.50f * h)
            lineTo(0.82f * w, 0.22f * h)
        }
        drawPath(d2, color = indexLineColor, style = mainStroke)

        // 3. Station interchange nodes (circular rings at transit junctions)
        val stations = listOf(
            androidx.compose.ui.geometry.Offset(0.24f * w, 0.50f * h),
            androidx.compose.ui.geometry.Offset(0.70f * w, 0.50f * h),
            androidx.compose.ui.geometry.Offset(0.12f * w, 0.22f * h),
            androidx.compose.ui.geometry.Offset(0.82f * w, 0.22f * h),
            androidx.compose.ui.geometry.Offset(0.36f * w, 0.78f * h),
            androidx.compose.ui.geometry.Offset(0.58f * w, 0.78f * h)
        )
        stations.forEach { pt ->
            // Outer station ring
            drawCircle(
                color = stationColor,
                radius = 3.2.dp.toPx(),
                center = pt,
                style = Stroke(width = 1.2.dp.toPx())
            )
            // Inner station node
            drawCircle(
                color = stationColor,
                radius = 1.4.dp.toPx(),
                center = pt
            )
        }
    }
}

/**
 * Universal Category-Aware Card Header pattern dispatcher.
 * Selects the differentiated pattern corresponding to the bike's category:
 * - MTB -> Topographic contour mountain map
 * - Road -> Aerodynamic hazard speed stripes
 * - Gravel -> Micro-tread & pebble matrix
 * - Commute -> Urban transit grid & metro lines
 */
@Composable
fun BikeCardCategoryHeader(
    category: BikeCategory,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
) {
    when (category) {
        BikeCategory.MTB -> BikeCardTopoHeader(modifier = modifier, height = height, shape = shape)
        BikeCategory.ROAD -> BikeCardHazardHeader(modifier = modifier, height = height, shape = shape)
        BikeCategory.GRAVEL -> BikeCardGravelHeader(modifier = modifier, height = height, shape = shape)
        BikeCategory.COMMUTE -> BikeCardCommuteHeader(modifier = modifier, height = height, shape = shape)
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    isCollapsible: Boolean = false,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null,
    collapsedSummary: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "SectionCardChevronRotation"
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val collapseDesc = stringResource(id = R.string.bike_collapse_section)
            val expandDesc = stringResource(id = R.string.bike_expand_section)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCollapsible && onToggleExpand != null) {
                            Modifier
                                .semantics {
                                    stateDescription = if (isExpanded) collapseDesc else expandDesc
                                }
                                .clickable(role = Role.Button) { onToggleExpand() }
                        } else Modifier
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                if (isCollapsible && onToggleExpand != null) {
                    IconButton(
                        onClick = onToggleExpand
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) collapseDesc else expandDesc,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isCollapsible || isExpanded,
                enter = expandVertically(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    animationSpec = tween(250, easing = LinearOutSlowInEasing)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec = tween(180, easing = FastOutLinearInEasing)
                )
            ) {
                Column {
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }

            if (isCollapsible && !isExpanded && collapsedSummary != null) {
                AnimatedVisibility(
                    visible = !isExpanded,
                    enter = expandVertically(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Top
                    ) + fadeIn(
                        animationSpec = tween(200, easing = LinearOutSlowInEasing)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(
                        animationSpec = tween(150, easing = FastOutLinearInEasing)
                    )
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        collapsedSummary()
                    }
                }
            }
        }
    }
}

/**
 * Ensures any focused input field remains visible and properly aligned above the soft keyboard
 * (or within the active scroll container) whenever the user taps it or the IME animates open.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keepVisibleAboveKeyboard(): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    LaunchedEffect(isFocused, imeBottom) {
        if (isFocused && imeBottom > 0) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    return this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged { focusState ->
            val nowFocused = focusState.isFocused || focusState.hasFocus
            isFocused = nowFocused
            if (nowFocused) {
                coroutineScope.launch {
                    delay(100)
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
}

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier.keepVisibleAboveKeyboard()) {
        OutlinedTextField(
            value = value,
            onValueChange = { newVal ->
                // N'accepter que les nombres positifs avec au plus une virgule/point
                val filtered = newVal.replace(',', '.')
                if (filtered.isEmpty() || filtered.matches(Regex("""^\d*\.?\d*$"""))) {
                    onValueChange(filtered)
                }
            },
            label = { Text(label) },
            suffix = { Text(suffix, style = MaterialTheme.typography.labelMedium) },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun <T> PillSelector(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    val tabs = androidx.compose.runtime.remember(items, labelProvider) {
        items.mapIndexed { index, item ->
            LightTab(
                id = index,
                title = labelProvider(item),
                description = labelProvider(item)
            )
        }
    }
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    LightTabs(
        tabs = tabs,
        selectedId = selectedIndex,
        onSelect = { id ->
            val index = id as? Int ?: return@LightTabs
            if (index in items.indices) {
                onItemSelected(items[index])
            }
        },
        fillWidth = true,
        height = 44.dp,
        modifier = modifier.fillMaxWidth(),
        haptics = true
    )
}

@Composable
fun WarningCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningAmberBg.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, WarningAmber)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(id = R.string.warning_title),
                tint = WarningAmber,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp, end = 8.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF78350F), // Brun foncé lisible
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Smooth top edge gradient fade indicating more content above during scroll.
 */
@Composable
fun BoxScope.ScrollTopFade(
    visible: Boolean,
    height: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .align(Alignment.TopCenter)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Smooth bottom edge gradient fade indicating more content below during scroll.
 * Optionally displays a floating "scroll for more" indicator pill if showIndicator = true.
 */
@Composable
fun BoxScope.ScrollBottomFade(
    visible: Boolean,
    height: Dp = 48.dp,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = false,
    indicatorText: String? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .align(Alignment.BottomCenter)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (showIndicator && indicatorText != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = indicatorText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Tire Pressure App Icon with Option B "Double Onde Sonique de Gonflage" (Double Concentric Sonic Pressure Wave).
 * A short tap triggers a punchy inflation pulse (expanding visibly to 1.20x like a high-pressure pump burst)
 * accompanied by two concentric circular shockwaves in dynamic accent color that burst outwards into the background,
 * synchronized with tactile bounce haptics.
 */
@Composable
fun TirePressurePulseLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    imageModifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val haptics = rememberBounceHaptics()
    val primaryColor = MaterialTheme.colorScheme.primary

    var pulseTrigger by remember { mutableIntStateOf(0) }
    val logoScale = remember { Animatable(1f) }
    val wave1Scale = remember { Animatable(1f) }
    val wave1Alpha = remember { Animatable(0f) }
    val wave2Scale = remember { Animatable(1f) }
    val wave2Alpha = remember { Animatable(0f) }

    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            // Punchy logo inflation pulse - expanding visibly to 1.20x and bouncing back
            launch {
                logoScale.snapTo(1f)
                logoScale.animateTo(
                    targetValue = 1.20f,
                    animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                )
                logoScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = 0.38f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            // Wave 1: First concentric circular sonic pressure wave
            launch {
                wave1Scale.snapTo(1.0f)
                wave1Alpha.snapTo(0.90f)
                launch {
                    wave1Scale.animateTo(
                        targetValue = 2.2f,
                        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    wave1Alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 520, easing = LinearOutSlowInEasing)
                    )
                }
            }
            // Wave 2: Second concentric circular wave, delayed by ~60ms and expanding to 2.8x
            launch {
                delay(60)
                wave2Scale.snapTo(1.0f)
                wave2Alpha.snapTo(0.70f)
                launch {
                    wave2Scale.animateTo(
                        targetValue = 2.8f,
                        animationSpec = tween(durationMillis = 580, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    wave2Alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 580, easing = LinearOutSlowInEasing)
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.bounce(1f, true)
                pulseTrigger++
            },
        contentAlignment = Alignment.Center
    ) {
        // Wave 2: Second outer concentric circular wave
        if (wave2Alpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = wave2Scale.value
                        scaleY = wave2Scale.value
                        alpha = wave2Alpha.value
                        clip = false
                    }
                    .border(
                        width = 3.dp,
                        color = primaryColor,
                        shape = CircleShape
                    )
                    .background(
                        color = primaryColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
            )
        }

        // Wave 1: First inner concentric circular wave
        if (wave1Alpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = wave1Scale.value
                        scaleY = wave1Scale.value
                        alpha = wave1Alpha.value
                        clip = false
                    }
                    .border(
                        width = 4.dp,
                        color = primaryColor,
                        shape = CircleShape
                    )
                    .background(
                        color = primaryColor.copy(alpha = 0.20f),
                        shape = CircleShape
                    )
            )
        }

        // App logo with Flight shared-element anchor and interactive tactile bounce
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .then(imageModifier)
                .clip(shape)
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
        )
    }
}

