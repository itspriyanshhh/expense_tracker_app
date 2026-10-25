package com.example.pulseledger.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseledger.ui.screens.dashboard.DashboardScreen
import com.example.pulseledger.ui.screens.memory.MerchantMemoryScreen
import com.example.pulseledger.ui.screens.settings.SettingsScreen
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.EmeraldNeonGlow
import com.example.pulseledger.ui.theme.GlassSlate
import com.example.pulseledger.ui.theme.GlassSlateElevated
import com.example.pulseledger.ui.theme.ObsidianSlate
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary

/**
 * 3 Primary navigation tabs for PulseLedger:
 * 1. Dashboard (Spend pulse, 1-tap P2P resolve, Bento Analytics, Timeline)
 * 2. Learned Merchants (Tier 1 Memory Vault, 0ms exact payee intelligence)
 * 3. Settings & Diagnostics (Live engine health, security, system controls)
 */
enum class PulseTab(
    val title: String,
    val shortTitle: String,
    val icon: ImageVector
) {
    DASHBOARD(
        title = "Dashboard",
        shortTitle = "Home",
        icon = Icons.Default.Home
    ),
    MERCHANT_MEMORY(
        title = "Learned Merchants",
        shortTitle = "Memory",
        icon = Icons.Default.Memory
    ),
    SETTINGS(
        title = "Settings & Diagnostics",
        shortTitle = "Settings",
        icon = Icons.Default.Tune
    )
}

/**
 * Main application shell wrapping the 3 primary screens and the floating Dark & Light Fintech bottom dock.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PulseMainShell(
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(PulseTab.DASHBOARD) }
    var isQuickAddOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSlate)
    ) {
        // Active Screen Content with Cross-fade
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                PulseTab.DASHBOARD -> {
                    DashboardScreen(
                        onOpenSettings = { selectedTab = PulseTab.SETTINGS },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                PulseTab.MERCHANT_MEMORY -> {
                    MerchantMemoryScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                PulseTab.SETTINGS -> {
                    SettingsScreen(
                        onOpenOnboarding = onOpenOnboarding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Floating Bottom Navigation Bar with Bottom Scrim
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Gradient Scrim to let content glide gracefully beneath
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                ObsidianSlate.copy(alpha = 0.75f),
                                ObsidianSlate
                            )
                        )
                    )
            )

            // Floating Navigation Bar Container
            PulseBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onCenterActionClick = { isQuickAddOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            )
        }

        // Global Quick Cash Add Sheet triggered from Center Dock Button
        if (isQuickAddOpen) {
            com.example.pulseledger.ui.screens.dashboard.components.QuickAddSheet(
                onDismiss = { isQuickAddOpen = false },
                onSaveCashExpense = { cashTx ->
                    isQuickAddOpen = false
                }
            )
        }
    }
}

/**
 * Floating curved bento bottom dock with elevated center action button.
 * Matches Image 1 (Electric Lime) & Image 2 (Deep Forest Green).
 */
@Composable
fun PulseBottomBar(
    selectedTab: PulseTab,
    onTabSelected: (PulseTab) -> Unit,
    onCenterActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dockShape = RoundedCornerShape(32.dp)
    val isDark = com.example.pulseledger.ui.theme.PulseTheme.colors.isDark

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(dockShape)
            .background(GlassSlate.copy(alpha = if (isDark) 0.96f else 0.98f))
            .border(1.dp, BorderSlate, dockShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Home / Dashboard
            DockTabItem(
                tab = PulseTab.DASHBOARD,
                isSelected = selectedTab == PulseTab.DASHBOARD,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(PulseTab.DASHBOARD)
                },
                modifier = Modifier.weight(1f)
            )

            // Tab 2: Vault / Learned Merchants
            DockTabItem(
                tab = PulseTab.MERCHANT_MEMORY,
                isSelected = selectedTab == PulseTab.MERCHANT_MEMORY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(PulseTab.MERCHANT_MEMORY)
                },
                modifier = Modifier.weight(1f)
            )

            // Center Elevated Action Button (Quick Add / Exchange Button from reference)
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(com.example.pulseledger.ui.theme.PillPrimary)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCenterActionClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Quick Add",
                    tint = com.example.pulseledger.ui.theme.PillOnPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab 3: Settings & Diagnostics
            DockTabItem(
                tab = PulseTab.SETTINGS,
                isSelected = selectedTab == PulseTab.SETTINGS,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(PulseTab.SETTINGS)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DockTabItem(
    tab: PulseTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.example.pulseledger.ui.theme.PulseTheme.colors.isDark

    val iconTint by animateColorAsState(
        targetValue = when {
            isSelected && isDark -> com.example.pulseledger.ui.theme.ElectricLime
            isSelected && !isDark -> com.example.pulseledger.ui.theme.DeepForestGreen
            else -> TextSecondary
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconTint"
    )

    val activeIndicatorBg by animateColorAsState(
        targetValue = when {
            isSelected && isDark -> com.example.pulseledger.ui.theme.ElectricLime.copy(alpha = 0.18f)
            isSelected && !isDark -> com.example.pulseledger.ui.theme.DeepForestGreen.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "activeIndicatorBg"
    )

    val textTint by animateColorAsState(
        targetValue = when {
            isSelected && isDark -> com.example.pulseledger.ui.theme.ElectricLime
            isSelected && !isDark -> com.example.pulseledger.ui.theme.DeepForestGreen
            else -> TextMuted
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "textTint"
    )

    val pillShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(pillShape)
            .background(activeIndicatorBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tab.shortTitle,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textTint,
                maxLines = 1
            )
        }
    }
}
