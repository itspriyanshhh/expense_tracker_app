package com.example.pulseledger.ui.screens.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseledger.data.local.model.CategorySpend
import com.example.pulseledger.ui.components.AnimatedCurrencyText
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.PulseTheme
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private data class ArcSlice(
    val category: PulseCategory,
    val amount: Double,
    val percentage: Double,
    val startAngle: Float,
    val sweepAngle: Float
)

/**
 * Interactive Donut Chart built natively with Compose Canvas and Animatable.
 *
 * Zero third-party chart dependencies.
 * Features:
 * - Rounded-cap arc sweeps with spring interpolation.
 * - Touch-to-select arc slices with 6dp outward radial offset and glowing highlight.
 * - Dynamic center label displaying category name, percentage, and animated currency total.
 * - Bidirectional filtering of the transaction timeline below.
 */
@Composable
fun DonutSpendChart(
    categorySpends: List<CategorySpend>,
    selectedCategory: PulseCategory?,
    onCategorySelected: (PulseCategory?) -> Unit,
    modifier: Modifier = Modifier,
    chartSize: Dp = 230.dp,
    strokeWidth: Dp = 22.dp
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val outwardOffsetPx = with(density) { 6.dp.toPx() }

    val isDark = PulseTheme.colors.isDark
    val donutTones = remember(isDark) {
        if (isDark) {
            listOf(
                Color(0xFFD4FF00), // Electric Lime (Signature highlight)
                Color(0xFFFFFFFF), // Crisp Pure White
                Color(0xFFA3E635), // Lime Mint
                Color(0xFF869584), // Olive Sage
                Color(0xFF4ADE80), // Emerald
                Color(0xFF5A6858)  // Deep Muted Olive
            )
        } else {
            listOf(
                Color(0xFF133E2B), // Royal Forest Green (Hero Card match)
                Color(0xFF1E523A), // Pine Green
                Color(0xFF2E7D52), // Emerald Forest
                Color(0xFF52946D), // Sage Green
                Color(0xFF85C99F), // Soft Mint
                Color(0xFFA3D9B8)  // Celadon Sprout
            )
        }
    }

    val totalSpend = remember(categorySpends) {
        categorySpends.sumOf { it.totalAmount }
    }

    // Spring sweep animation on enter / dataset updates
    val sweepAnimatable = remember { Animatable(0f) }
    LaunchedEffect(categorySpends) {
        sweepAnimatable.snapTo(0f)
        sweepAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    // Precalculate arc geometry
    val arcSlices = remember(categorySpends, totalSpend) {
        if (totalSpend <= 0.0) return@remember emptyList()

        val slices = mutableListOf<ArcSlice>()
        var currentStart = -90f // Start from 12 o'clock
        val gapDegrees = if (categorySpends.size > 1) 4f else 0f

        categorySpends.forEach { spend ->
            val fraction = (spend.totalAmount / totalSpend).coerceIn(0.0, 1.0)
            val sweep = (fraction * 360f).toFloat()
            val effectiveSweep = (sweep - gapDegrees).coerceAtLeast(1f)
            val cat = PulseCategory.fromString(spend.category)

            slices.add(
                ArcSlice(
                    category = cat,
                    amount = spend.totalAmount,
                    percentage = fraction * 100,
                    startAngle = currentStart,
                    sweepAngle = effectiveSweep
                )
            )
            currentStart += sweep
        }
        slices
    }

    val emptyRingColor = BorderSlate.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartSize),
        contentAlignment = Alignment.Center
    ) {
        // Native Canvas Drawing Arc Slices
        Canvas(
            modifier = Modifier
                .size(chartSize)
                .pointerInput(arcSlices) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = tapOffset.x - centerX
                        val dy = tapOffset.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)

                        val baseRadius = (min(size.width, size.height) - strokeWidthPx - (outwardOffsetPx * 2)) / 2f
                        val innerRadius = baseRadius - (strokeWidthPx / 2f) - 25f
                        val outerRadius = baseRadius + (strokeWidthPx / 2f) + outwardOffsetPx + 25f

                        if (distance in innerRadius..outerRadius) {
                            // Calculate tap angle normalized from 12 o'clock (-90 deg to 270 deg)
                            var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (touchAngle < -90f) {
                                touchAngle += 360f
                            }

                            val matchedSlice = arcSlices.firstOrNull { slice ->
                                val endAngle = slice.startAngle + slice.sweepAngle
                                touchAngle in slice.startAngle..endAngle
                            }

                            if (matchedSlice != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (selectedCategory == matchedSlice.category) {
                                    onCategorySelected(null)
                                } else {
                                    onCategorySelected(matchedSlice.category)
                                }
                            }
                        } else {
                            // Tap outside resets filter
                            if (selectedCategory != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCategorySelected(null)
                            }
                        }
                    }
                }
        ) {
            val progress = sweepAnimatable.value
            val baseRadius = (min(size.width, size.height) - strokeWidthPx - (outwardOffsetPx * 2)) / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            // If empty dataset: draw subtle placeholder ring
            if (arcSlices.isEmpty()) {
                drawCircle(
                    color = emptyRingColor,
                    radius = baseRadius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
                return@Canvas
            }

            // Draw Category Arcs
            arcSlices.forEachIndexed { index, slice ->
                val isSelected = selectedCategory == slice.category
                val sliceColor = donutTones[index % donutTones.size]

                // Outward radial shift when slice is tapped
                val midAngleRad = Math.toRadians((slice.startAngle + slice.sweepAngle / 2f).toDouble())
                val shift = if (isSelected) outwardOffsetPx else 0f
                val sliceCenterX = centerOffset.x + (cos(midAngleRad) * shift).toFloat()
                val sliceCenterY = centerOffset.y + (sin(midAngleRad) * shift).toFloat()

                val sliceStrokeWidth = if (isSelected) strokeWidthPx + with(density) { 4.dp.toPx() } else strokeWidthPx
                val arcRadius = baseRadius

                val animatedSweep = slice.sweepAngle * progress

                // Optional halo glow for selected arc
                if (isSelected) {
                    drawArc(
                        color = sliceColor.copy(alpha = 0.25f),
                        startAngle = slice.startAngle,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = Offset(sliceCenterX - arcRadius, sliceCenterY - arcRadius),
                        size = Size(arcRadius * 2, arcRadius * 2),
                        style = Stroke(width = sliceStrokeWidth + with(density) { 10.dp.toPx() }, cap = StrokeCap.Round)
                    )
                }

                drawArc(
                    color = if (selectedCategory == null || isSelected) sliceColor else sliceColor.copy(alpha = 0.35f),
                    startAngle = slice.startAngle,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(sliceCenterX - arcRadius, sliceCenterY - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = sliceStrokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content Overlay
        Column(
            modifier = Modifier
                .size(chartSize - (strokeWidth * 2) - 32.dp)
                .clip(CircleShape)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "center_chart_label"
            ) { activeCategory ->
                if (activeCategory != null) {
                    val sliceIndex = arcSlices.indexOfFirst { it.category == activeCategory }
                    val activeSliceColor = if (sliceIndex >= 0) donutTones[sliceIndex % donutTones.size] else activeCategory.color
                    val slice = if (sliceIndex >= 0) arcSlices[sliceIndex] else null
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = activeCategory.icon,
                                contentDescription = null,
                                tint = activeSliceColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeCategory.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeSliceColor,
                                maxLines = 1
                            )
                        }

                        AnimatedCurrencyText(
                            amount = slice?.amount ?: 0.0,
                            includeDecimals = false,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Text(
                            text = "${slice?.percentage?.toInt() ?: 0}% of spend",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedCurrencyText(
                            amount = totalSpend,
                            includeDecimals = false,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFeatureSettings = com.example.pulseledger.ui.theme.TABULAR_NUMBERS_FEATURE
                            )
                        )

                        Text(
                            text = "Total Spent",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
