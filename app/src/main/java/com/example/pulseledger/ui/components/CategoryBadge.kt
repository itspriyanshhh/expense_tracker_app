package com.example.pulseledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.GlassSlateElevated
import com.example.pulseledger.ui.theme.GlowingAmber
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary

/**
 * Compact Category Badge displaying category icon and title with tinted glow.
 */
@Composable
fun CategoryBadge(
    category: PulseCategory,
    modifier: Modifier = Modifier,
    isPending: Boolean = category.isPendingReview,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val accentColor = if (isPending) GlowingAmber else category.color
    val shape = RoundedCornerShape(8.dp)

    val clickableModifier = if (onClick != null) {
        Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    } else Modifier

    Row(
        modifier = modifier
            .clip(shape)
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), shape)
            .then(clickableModifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.displayName,
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = category.displayName,
            color = if (isPending) GlowingAmber else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Interactive quick-categorize chip for 1-Tap categorizing flows.
 */
@Composable
fun QuickCategorizeChip(
    category: PulseCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(12.dp)
    val accentColor = category.color

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (isSelected) accentColor.copy(alpha = 0.25f)
                else GlassSlateElevated
            )
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else BorderSlate,
                shape = shape
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.displayName,
                tint = if (isSelected) accentColor else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = category.displayName,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
