package com.example.pulseledger.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.pulseledger.R
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.ObsidianSlate

/**
 * Reusable SpendLog Brand Icon featuring the official Neon Shield Check Ring asset
 * framed within a subtle breathing emerald glow aura and sleek dark fintech container.
 *
 * @param modifier Custom modifier for dimensions and layout.
 * @param animatePulse Whether the outer ambient emerald glow gently breathes/pulses.
 */
@Composable
fun SpendLogBrandIcon(
    modifier: Modifier = Modifier,
    animatePulse: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SpendLogPulseTransition")

    val pulseScale by if (animatePulse) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    val glowAlpha by if (animatePulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.42f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0.25f) }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer breathing emerald glow aura
        Box(
            modifier = Modifier
                .fillMaxSize(0.96f)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldNeon.copy(alpha = glowAlpha),
                            EmeraldNeon.copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Obsidian container card with subtle border
        Box(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .clip(CircleShape)
                .background(ObsidianSlate)
                .border(
                    width = 1.dp,
                    color = BorderSlate.copy(alpha = 0.8f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // High-resolution WebP brand asset
            Image(
                painter = painterResource(id = R.drawable.brand_logo_neon),
                contentDescription = "SpendLog Brand Icon",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            )
        }
    }
}
