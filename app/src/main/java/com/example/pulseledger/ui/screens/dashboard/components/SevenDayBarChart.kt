package com.example.pulseledger.ui.screens.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseledger.data.local.model.DailySpend
import com.example.pulseledger.ui.components.AnimatedCurrencyText
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.EmeraldNeonGlow
import com.example.pulseledger.ui.theme.GlassSlateElevated
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 7-Day Spend Pulse Bar Chart with touch scrubbing and floating glass tooltip.
 *
 * Built entirely with Compose Canvas and Animatable — zero 3rd-party dependencies.
 */
@Composable
fun SevenDayBarChart(
    dailySpends: List<DailySpend>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    val heightAnim = remember { Animatable(0f) }
    LaunchedEffect(dailySpends) {
        heightAnim.snapTo(0f)
        heightAnim.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val maxSpend = remember(dailySpends) {
        dailySpends.maxOfOrNull { it.totalAmount }?.coerceAtLeast(100.0) ?: 100.0
    }

    val dayLabels = remember(dailySpends) {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
        val fullDateFormat = SimpleDateFormat("EEE, d MMM", Locale.ENGLISH)

        dailySpends.map { spend ->
            val date = try {
                inputFormat.parse(spend.dateString)
            } catch (e: Exception) {
                null
            }
            val shortDay = if (date != null) dayOfWeekFormat.format(date) else ""
            val fullDay = if (date != null) fullDateFormat.format(date) else spend.dateString
            Pair(shortDay, fullDay)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(top = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Floating Tooltip Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = scrubbedIndex != null,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(200))
                ) {
                    val activeIdx = scrubbedIndex ?: 0
                    val activeSpend = dailySpends.getOrNull(activeIdx)
                    val activeDayLabel = dayLabels.getOrNull(activeIdx)?.second.orEmpty()

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSlateElevated)
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = activeDayLabel,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            AnimatedCurrencyText(
                                amount = activeSpend?.totalAmount ?: 0.0,
                                includeDecimals = false,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldNeon
                                )
                            )
                        }
                    }
                }

                if (scrubbedIndex == null) {
                    Text(
                        text = "Touch and scrub to inspect daily spend",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val trackColor = com.example.pulseledger.ui.theme.ChartTrackColor
            val emeraldAccent = com.example.pulseledger.ui.theme.ChartSelectedColor

            // Canvas Drawing the 7 Vertical Rounded Gradient Bars
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(dailySpends) {
                        detectTapGestures(
                            onPress = { offset ->
                                val slotWidth = size.width / 7f
                                val idx = (offset.x / slotWidth).toInt().coerceIn(0, 6)
                                if (idx != scrubbedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scrubbedIndex = idx
                                }
                            }
                        )
                    }
                    .pointerInput(dailySpends) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val slotWidth = size.width / 7f
                                val idx = (offset.x / slotWidth).toInt().coerceIn(0, 6)
                                if (idx != scrubbedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scrubbedIndex = idx
                                }
                            },
                            onDragEnd = {
                                // Keep selected or retain for reading
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val slotWidth = size.width / 7f
                                val idx = (change.position.x / slotWidth).toInt().coerceIn(0, 6)
                                if (idx != scrubbedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scrubbedIndex = idx
                                }
                            }
                        )
                    }
            ) {
                val anim = heightAnim.value
                val slotWidth = size.width / 7f
                val barWidth = with(density) { 22.dp.toPx() }
                val maxBarHeight = size.height - with(density) { 8.dp.toPx() }
                val minPillHeight = with(density) { 5.dp.toPx() }
                val cornerRadius = CornerRadius(with(density) { 6.dp.toPx() })

                dailySpends.forEachIndexed { i, spend ->
                    val isSelected = scrubbedIndex == i
                    val fraction = (spend.totalAmount / maxSpend).coerceIn(0.0, 1.0).toFloat()
                    val barHeight = (fraction * maxBarHeight * anim).coerceAtLeast(minPillHeight)

                    val slotCenterX = (i * slotWidth) + (slotWidth / 2f)
                    val barLeft = slotCenterX - (barWidth / 2f)
                    val barTop = size.height - barHeight

                    val barBrush = if (isSelected) {
                        Brush.verticalGradient(
                            listOf(
                                emeraldAccent,
                                emeraldAccent.copy(alpha = 0.65f)
                            ),
                            startY = barTop,
                            endY = size.height
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                emeraldAccent.copy(alpha = 0.85f),
                                emeraldAccent.copy(alpha = 0.25f)
                            ),
                            startY = barTop,
                            endY = size.height
                        )
                    }

                    // Background track slot for contrast
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(barLeft, with(density) { 4.dp.toPx() }),
                        size = Size(barWidth, size.height - with(density) { 4.dp.toPx() }),
                        cornerRadius = cornerRadius
                    )

                    // Active Spend Bar
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )

                    // Highlight Ring for Scrubbed Bar
                    if (isSelected) {
                        drawRoundRect(
                            color = emeraldAccent,
                            topLeft = Offset(barLeft - with(density) { 2.dp.toPx() }, barTop - with(density) { 2.dp.toPx() }),
                            size = Size(barWidth + with(density) { 4.dp.toPx() }, barHeight + with(density) { 4.dp.toPx() }),
                            cornerRadius = CornerRadius(with(density) { 8.dp.toPx() }),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(with(density) { 1.5.dp.toPx() })
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Day Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayLabels.forEachIndexed { idx, pair ->
                    val isSelected = scrubbedIndex == idx
                    Text(
                        text = pair.first,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) EmeraldNeon else TextSecondary,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
