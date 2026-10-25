package com.example.pulseledger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.GlassSlate
import com.example.pulseledger.ui.theme.PulseTheme

/**
 * Reusable Elevated Glassmorphic Card for the Dark & Light Fintech Craft aesthetic.
 *
 * Provides an elevated surface with a subtle 1dp border and an ambient top-down linear sheen.
 */
@Composable
fun PulseGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSlate,
    borderColor: Color = BorderSlate,
    borderWidth: Dp = 1.dp,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    enableHapticsOnClick: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = PulseTheme.colors.isDark

    // Subtle glass sheen gradient: soft top highlight fading to surface color
    val glassSheenBrush = remember(backgroundColor, isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.98f),
                    backgroundColor.copy(alpha = 0.90f),
                    backgroundColor
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = 0.96f),
                    backgroundColor
                )
            )
        }
    }

    val effectiveBorderColor = glowColor ?: borderColor

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null // Custom glass press indication handled or subtle ripple
        ) {
            if (enableHapticsOnClick) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                BorderStroke(borderWidth, effectiveBorderColor),
                shape = shape
            )
            .then(clickableModifier),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(glassSheenBrush, shape = shape)
        ) {
            // Optional subtle ambient glow indicator in top border
            if (glowColor != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = if (isDark) 0.08f else 0.04f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 80f
                            ),
                            shape = shape
                        )
                )
            }
            content()
        }
    }
}
