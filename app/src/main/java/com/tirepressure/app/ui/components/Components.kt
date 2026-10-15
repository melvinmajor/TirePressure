package com.tirepressure.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
 * Reusable Glassmorphic 3D Card with translucent gradient surface, specular highlight border,
 * and soft ambient/spot depth shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 3D volume shadow with enhanced ambient & spot diffusion
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.60f) else Color(0xFF475569).copy(alpha = 0.18f)
    val ambientColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF475569).copy(alpha = 0.10f)

    // Frosted glass gradient background with enhanced translucency
    val glassBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF222C3D).copy(alpha = 0.72f),
                Color(0xFF131924).copy(alpha = 0.78f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.82f),
                Color(0xFFF1F5F9).copy(alpha = 0.74f)
            )
        )
    }

    // Frosted specular rim border gradient
    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFCBD5E1).copy(alpha = 0.45f),
                Color(0xFF94A3B8).copy(alpha = 0.20f)
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

/**
 * Reusable Frosted Glass Alert Dialog with translucent surface, specular rim border,
 * soft ambient/spot depth shadow, and 24.dp rounded corners.
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
                Color(0xFF222C3D).copy(alpha = 0.85f),
                Color(0xFF131924).copy(alpha = 0.90f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color(0xFFF1F5F9).copy(alpha = 0.86f)
            )
        )
    }

    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFCBD5E1).copy(alpha = 0.65f),
                Color(0xFF94A3B8).copy(alpha = 0.30f)
            )
        )
    }

    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color(0xFF475569).copy(alpha = 0.22f)

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
        modifier = modifier
            .shadow(24.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .background(glassBrush, shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCollapsible && onToggleExpand != null) {
                            Modifier.clickable { onToggleExpand() }
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isCollapsible && onToggleExpand != null) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
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
    Column(modifier = modifier) {
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(200),
                label = "PillBgColor"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "PillTextColor"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onItemSelected(item) }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = labelProvider(item),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
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
                contentDescription = "Avertissement",
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

