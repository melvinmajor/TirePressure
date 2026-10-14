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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

/**
 * Reusable Glassmorphic 3D Card with translucent gradient surface, specular highlight border,
 * and soft ambient/spot depth shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: androidx.compose.ui.unit.Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 3D volume shadow
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color(0xFF64748B).copy(alpha = 0.14f)
    val ambientColor = if (isDark) Color.Black.copy(alpha = 0.30f) else Color(0xFF64748B).copy(alpha = 0.08f)

    // Frosted glass gradient background
    val glassBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1F2734).copy(alpha = 0.88f),
                Color(0xFF131822).copy(alpha = 0.92f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFF3F6F9).copy(alpha = 0.88f)
            )
        )
    }

    // Frosted specular rim border gradient
    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFCBD5E1).copy(alpha = 0.55f)
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
 * Elegant Topographic Lines Header pattern for bike overview cards.
 * Features subtle, clean contour elevation lines on a flat background with flat bottom boundary.
 */
@Composable
fun BikeCardTopoHeader(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Subtle, clean contour line colors (intermediate vs index contours)
    val intermediateColor = if (isDark) {
        Color.White.copy(alpha = 0.055f)
    } else {
        Color(0xFF334155).copy(alpha = 0.075f)
    }

    val indexColor = if (isDark) {
        Color.White.copy(alpha = 0.095f)
    } else {
        Color(0xFF1E293B).copy(alpha = 0.12f)
    }

    // Flat subtle background tint (clean, uniform, flat with zero 3D rounded gradient)
    val flatBgColor = if (isDark) {
        Color.White.copy(alpha = 0.018f)
    } else {
        Color(0xFF0F172A).copy(alpha = 0.022f)
    }

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

        val thinStroke = Stroke(
            width = 1.0.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val indexStroke = Stroke(
            width = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // 2. Topographic contour lines
        // Contour 1 (near top edge)
        val p1 = Path().apply {
            moveTo(0f, 0.16f * h)
            cubicTo(0.18f * w, 0.06f * h, 0.36f * w, 0.26f * h, 0.52f * w, 0.14f * h)
            cubicTo(0.66f * w, 0.04f * h, 0.82f * w, 0.20f * h, w, 0.10f * h)
        }
        drawPath(p1, color = intermediateColor, style = thinStroke)

        // Contour 2 (Index contour)
        val p2 = Path().apply {
            moveTo(0f, 0.34f * h)
            cubicTo(0.16f * w, 0.20f * h, 0.34f * w, 0.44f * h, 0.50f * w, 0.30f * h)
            cubicTo(0.64f * w, 0.18f * h, 0.78f * w, 0.18f * h, w, 0.25f * h)
        }
        drawPath(p2, color = indexColor, style = indexStroke)

        // Contour 3 (Flowing around ridge feature on right)
        val p3 = Path().apply {
            moveTo(0f, 0.52f * h)
            cubicTo(0.15f * w, 0.36f * h, 0.30f * w, 0.62f * h, 0.46f * w, 0.48f * h)
            cubicTo(0.60f * w, 0.34f * h, 0.74f * w, 0.36f * h, 0.84f * w, 0.32f * h)
            cubicTo(0.92f * w, 0.30f * h, 0.96f * w, 0.38f * h, w, 0.42f * h)
        }
        drawPath(p3, color = intermediateColor, style = thinStroke)

        // Contour 4 (Summit loop on right knoll: outer and inner contour)
        val pSummitOuter = Path().apply {
            val cx = 0.80f * w
            val cy = 0.56f * h
            val rx = 0.07f * w
            val ry = 0.17f * h
            moveTo(cx - rx, cy)
            cubicTo(cx - rx, cy - ry, cx + rx, cy - ry, cx + rx, cy)
            cubicTo(cx + rx, cy + ry, cx - rx, cy + ry, cx - rx, cy)
            close()
        }
        drawPath(pSummitOuter, color = intermediateColor, style = thinStroke)

        val pSummitInner = Path().apply {
            val cx = 0.80f * w
            val cy = 0.56f * h
            val rx = 0.035f * w
            val ry = 0.08f * h
            moveTo(cx - rx, cy)
            cubicTo(cx - rx, cy - ry, cx + rx, cy - ry, cx + rx, cy)
            cubicTo(cx + rx, cy + ry, cx - rx, cy + ry, cx - rx, cy)
            close()
        }
        drawPath(pSummitInner, color = indexColor, style = indexStroke)

        // Contour 5 (Lower contour wrapping under summit)
        val p5 = Path().apply {
            moveTo(0f, 0.70f * h)
            cubicTo(0.18f * w, 0.54f * h, 0.34f * w, 0.80f * h, 0.50f * w, 0.66f * h)
            cubicTo(0.64f * w, 0.52f * h, 0.72f * w, 0.78f * h, 0.84f * w, 0.78f * h)
            cubicTo(0.92f * w, 0.78f * h, 0.96f * w, 0.66f * h, w, 0.60f * h)
        }
        drawPath(p5, color = intermediateColor, style = thinStroke)

        // Contour 6 (Bottom index contour)
        val p6 = Path().apply {
            moveTo(0f, 0.88f * h)
            cubicTo(0.18f * w, 0.74f * h, 0.36f * w, 0.94f * h, 0.54f * w, 0.84f * h)
            cubicTo(0.68f * w, 0.76f * h, 0.82f * w, 0.94f * h, w, 0.80f * h)
        }
        drawPath(p6, color = indexColor, style = indexStroke)
    }
}

/**
 * Backward-compatible alias for BikeCardTopoHeader.
 */
@Composable
fun BikeCardHazardHeader(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
) = BikeCardTopoHeader(modifier = modifier, height = height, shape = shape)

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

