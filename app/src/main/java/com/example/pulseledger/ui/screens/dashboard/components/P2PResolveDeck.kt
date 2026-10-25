package com.example.pulseledger.ui.screens.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.service.ActionableNotificationHelper
import com.example.pulseledger.ui.components.PulseGlassCard
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.GlassSlateElevated
import com.example.pulseledger.ui.theme.GlowingAmber
import com.example.pulseledger.ui.theme.GlowingAmberGlow
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1-Tap P2P Resolve Deck.
 *
 * Sticky review card for transactions in PENDING_USER_TAP state.
 * Allows instant 1-tap categorization with tactile haptics, animating the card out
 * upon resolution and permanently memorizing the rule in Tier 1 Room DB.
 */
@Composable
fun P2PResolveDeck(
    pendingTransactions: List<TransactionEntity>,
    onResolve: (txId: String, lookupKey: String, category: String, customAlias: String?) -> Unit,
    onOpenEdit: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pendingTransactions.isEmpty()) return

    val currentTx = pendingTransactions.first()
    var isDismissing by remember(currentTx.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val timeFormatted = remember(currentTx.timestamp) {
        SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(currentTx.timestamp))
    }

    val contextualActions = remember(currentTx.amount, currentTx.timestamp) {
        ActionableNotificationHelper.getContextualActions(currentTx.amount, currentTx.timestamp)
    }

    AnimatedVisibility(
        visible = !isDismissing,
        enter = fadeIn(tween(250)),
        exit = scaleOut(targetScale = 0.85f, animationSpec = tween(250)) + fadeOut(tween(200)),
        modifier = modifier.fillMaxWidth()
    ) {
        PulseGlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GlowingAmber.copy(alpha = 0.5f),
            glowColor = GlowingAmberGlow,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header Pill: Pending Tap Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GlowingAmber)
                        )
                        Text(
                            text = "Unverified P2P Payment",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowingAmber,
                            letterSpacing = 0.4.sp
                        )
                    }

                    if (pendingTransactions.size > 1) {
                        Text(
                            text = "1 of ${pendingTransactions.size} pending",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Main Info Row: Amount & Merchant
                Column {
                    Text(
                        text = "Paid ₹${currentTx.amount.toInt()} to ${currentTx.displayMerchant}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$timeFormatted via ${currentTx.bankOrAppSource}${currentTx.upiVpa?.let { " • $it" } ?: ""}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Horizontal Row of 1-Tap Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    contextualActions.forEach { action ->
                        QuickResolveChip(
                            label = action.label,
                            color = action.category.color,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isDismissing = true
                                val lookupKey = currentTx.upiVpa ?: currentTx.rawMerchant
                                onResolve(
                                    currentTx.id,
                                    lookupKey,
                                    action.category.id,
                                    currentTx.displayMerchant
                                )
                            }
                        )
                    }

                    // Rename + Tag Chip (Opens Bottom Sheet)
                    QuickResolveChip(
                        label = "✏️ Rename + Tag",
                        color = TextSecondary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenEdit(currentTx)
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickResolveChip(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(GlassSlateElevated)
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
