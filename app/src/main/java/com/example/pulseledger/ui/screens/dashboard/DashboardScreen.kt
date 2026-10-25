package com.example.pulseledger.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.ui.components.AnimatedCurrencyText
import com.example.pulseledger.ui.components.PulseGlassCard
import com.example.pulseledger.ui.components.TransactionDirection
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.ui.screens.dashboard.components.DonutSpendChart
import com.example.pulseledger.ui.screens.dashboard.components.EditMerchantBottomSheet
import com.example.pulseledger.ui.screens.dashboard.components.P2PResolveDeck
import com.example.pulseledger.ui.screens.dashboard.components.QuickAddSheet
import com.example.pulseledger.ui.screens.dashboard.components.SevenDayBarChart
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.CrimsonCoral
import com.example.pulseledger.ui.theme.CrimsonCoralGlow
import com.example.pulseledger.ui.theme.DeepForestGreen
import com.example.pulseledger.ui.theme.ElectricLime
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.EmeraldNeonGlow
import com.example.pulseledger.ui.theme.GlassSlate
import com.example.pulseledger.ui.theme.GlassSlateElevated
import com.example.pulseledger.ui.theme.GlowingAmber
import com.example.pulseledger.ui.theme.GlowingAmberGlow
import com.example.pulseledger.ui.theme.HeroCardBorder
import com.example.pulseledger.ui.theme.HeroCardPillBg
import com.example.pulseledger.ui.theme.HeroCardPillBorder
import com.example.pulseledger.ui.theme.HeroCardPillText
import com.example.pulseledger.ui.theme.HeroCardSurface
import com.example.pulseledger.ui.theme.HeroCardTextPrimary
import com.example.pulseledger.ui.theme.HeroCardTextSecondary
import com.example.pulseledger.ui.theme.ObsidianSlate
import com.example.pulseledger.ui.theme.PillOnPrimary
import com.example.pulseledger.ui.theme.PillPrimary
import com.example.pulseledger.ui.theme.PulseTheme
import com.example.pulseledger.ui.theme.TABULAR_NUMBERS_FEATURE
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Flagship Home Dashboard Screen.
 *
 * Built with Dark Fintech Craft aesthetic:
 * 1. Animated Hero Currency Ticker with velocity pill & verification badge.
 * 2. 1-Tap P2P Resolve Deck (sticky banner for pending P2P payments).
 * 3. Interactive Analytics Bento Grid (toggle between custom Canvas Donut and 7-Day Bar scrub chart).
 * 4. Smart Grouped Transaction Timeline with sticky date headers.
 * 5. Rule Customization Bottom Sheet for retroactive renaming and category updates.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var isQuickAddOpen by remember { mutableStateOf(false) }
    var isTripModeOpen by remember { mutableStateOf(false) }
    var isBudgetDialogOpen by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianSlate),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSlate)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            // 1. Top Hero Header & Spend Pulse
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 12.dp)
                ) {
                    // App Bar matching reference design: Greeting + Circular Action Icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Welcome back",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Text(
                                text = "Financial Pulse",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = (-0.25).sp
                            )
                        }

                        // Settings circular button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GlassSlateElevated)
                                .border(1.dp, BorderSlate, CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenSettings()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hero Balance Card: Royal Forest Green in Light Mode, Dark Olive Slate in Dark Mode
                    PulseGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = HeroCardSurface,
                        borderColor = HeroCardBorder
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row: Total Spend Label + Velocity Badge
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
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(HeroCardPillText)
                                    )
                                    Text(
                                        text = "Total Spend",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HeroCardTextSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                // Velocity vs Last Month Pill
                                val velocitySign = if (state.isSpendingHigher) "+" else "-"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(HeroCardPillBg)
                                        .border(1.dp, HeroCardPillBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$velocitySign${state.spendVelocityPercent}% vs last month",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HeroCardPillText
                                    )
                                }
                            }

                            // Big Currency Amount Display (White on Forest Green, Off-white on Dark Olive)
                            AnimatedCurrencyText(
                                amount = state.monthExpenses,
                                includeDecimals = true,
                                style = TextStyle(
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HeroCardTextPrimary,
                                    letterSpacing = (-0.5).sp,
                                    fontFeatureSettings = TABULAR_NUMBERS_FEATURE
                                )
                            )

                            // Bottom Row inside Hero: Safe Today Pill + Verification Status Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val safeText = if (state.safeToSpendToday <= 0) {
                                    "⚠️ Budget Exceeded"
                                } else {
                                    "🛡️ Safe Today: ₹${state.safeToSpendToday.toInt()}/d"
                                }

                                // Safe-to-Spend Pill
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(HeroCardPillBg)
                                        .border(1.dp, HeroCardPillBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isBudgetDialogOpen = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = safeText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HeroCardPillText,
                                            style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
                                        )
                                        Text(
                                            text = "✎ Budget",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HeroCardTextSecondary
                                        )
                                    }
                                }

                                // Verification Pill
                                val isAllVerified = state.pendingCount == 0
                                val verifyText = if (isAllVerified) "● 100% Verified" else "● ${state.pendingCount} Pending Tap"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(HeroCardPillBg)
                                        .border(1.dp, HeroCardPillBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = verifyText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HeroCardPillText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-Action Quick Bento Row (Matching Reference Design)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Add Cash Expense
                        QuickActionSquircle(
                            icon = Icons.Default.Add,
                            label = "Add Cash",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isQuickAddOpen = true
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 2. Trip / Event Mode
                        val isEventActive = state.activeEventTag != null
                        QuickActionSquircle(
                            icon = Icons.Default.FilterList,
                            label = if (isEventActive) state.activeEventTag ?: "Trip" else "Trip Mode",
                            isActive = isEventActive,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isTripModeOpen = true
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 3. Monthly Variable Budget
                        QuickActionSquircle(
                            icon = Icons.Default.Settings,
                            label = "Budget",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isBudgetDialogOpen = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Under-budget Insight Card (Matching Image 2 Analytics insight)
                    if (state.safeToSpendToday > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PulseGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = if (PulseTheme.colors.isDark) GlassSlateElevated else Color(0xFFEBF3ED),
                            borderColor = if (PulseTheme.colors.isDark) BorderSlate else Color(0xFFCFDFC8)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldNeon.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🌿", fontSize = 15.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "You're under budget!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (PulseTheme.colors.isDark) TextPrimary else Color(0xFF133E2B)
                                    )
                                    Text(
                                        text = "Safe daily spend is ₹${state.safeToSpendToday.toInt()}/day for remaining ${state.remainingDaysInMonth} days.",
                                        fontSize = 11.sp,
                                        color = if (PulseTheme.colors.isDark) TextSecondary else Color(0xFF3B6149)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1.5. 48-Hour Renewal Radar Banner (Step 8.5 Mandate Interceptor)
            if (state.activeMandates.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.activeMandates.forEach { mandate ->
                            val dateLabel = SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).format(Date(mandate.dueDate))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlowingAmberGlow)
                                    .border(1.dp, GlowingAmber.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(GlowingAmber.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = GlowingAmber,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = "48-Hour Renewal Radar",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GlowingAmber,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Text(
                                            text = "Due $dateLabel",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mandate.merchant,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Scheduled e-mandate / AutoPay",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "₹${mandate.amount.toInt()}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.dismissMandate(mandate.id)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "✕ Dismiss",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextMuted
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GlowingAmber)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.markMandatePaid(mandate.id)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "✓ Mark Paid",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ObsidianSlate
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. The 1-Tap P2P Resolve Deck (Conditional Sticky Banner)
            if (state.pendingTransactions.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        P2PResolveDeck(
                            pendingTransactions = state.pendingTransactions,
                            onResolve = { txId, lookupKey, category, customAlias ->
                                viewModel.resolveTransaction(txId, lookupKey, category, customAlias)
                            },
                            onOpenEdit = { tx ->
                                viewModel.openEditBottomSheet(tx)
                            }
                        )
                    }
                }
            }

            // 3. Interactive Analytics Bento Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    PulseGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Bento Header: Title + Toggleable Segmented Control
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state.chartViewMode == ChartViewMode.CATEGORY_DONUT) "Category Spend" else "7-Day Spend Pulse",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                // Segmented Control
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(GlassSlateElevated)
                                        .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    SegmentButton(
                                        icon = Icons.Default.PieChart,
                                        isSelected = state.chartViewMode == ChartViewMode.CATEGORY_DONUT,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.setChartViewMode(ChartViewMode.CATEGORY_DONUT)
                                        }
                                    )

                                    SegmentButton(
                                        icon = Icons.Default.BarChart,
                                        isSelected = state.chartViewMode == ChartViewMode.SPEND_PULSE_7_DAY,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.setChartViewMode(ChartViewMode.SPEND_PULSE_7_DAY)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Chart Canvas
                            when (state.chartViewMode) {
                                ChartViewMode.CATEGORY_DONUT -> {
                                    DonutSpendChart(
                                        categorySpends = state.categorySpends,
                                        selectedCategory = state.selectedCategoryFilter,
                                        onCategorySelected = { cat ->
                                            viewModel.setCategoryFilter(cat)
                                        }
                                    )
                                }

                                ChartViewMode.SPEND_PULSE_7_DAY -> {
                                    SevenDayBarChart(
                                        dailySpends = state.dailySpends
                                    )
                                }
                            }

                            // Active Filter Dismiss Bar (Category or Event Tag)
                            AnimatedVisibility(visible = state.selectedCategoryFilter != null || state.selectedEventTagFilter != null) {
                                val filterLabel = state.selectedEventTagFilter?.let { "Event: $it" }
                                    ?: state.selectedCategoryFilter?.displayName ?: ""
                                val filterColor = if (state.selectedEventTagFilter != null) GlowingAmber else state.selectedCategoryFilter?.color ?: EmeraldNeon

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(filterColor.copy(alpha = 0.15f))
                                        .border(1.dp, filterColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.setCategoryFilter(null)
                                            viewModel.setSelectedEventTagFilter(null)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Filtering: $filterLabel",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = filterColor
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Filter",
                                        tint = filterColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Default Category Filter Carousel on Homepage
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATEGORIES & EVENTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        if (state.selectedCategoryFilter != null || state.selectedEventTagFilter != null) {
                            Text(
                                text = "Show All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldNeon,
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setCategoryFilter(null)
                                    viewModel.setSelectedEventTagFilter(null)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "All" chip
                        val isAllSelected = state.selectedCategoryFilter == null && state.selectedEventTagFilter == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAllSelected) EmeraldNeon.copy(alpha = 0.2f) else GlassSlateElevated)
                                .border(
                                    1.dp,
                                    if (isAllSelected) EmeraldNeon else BorderSlate,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setCategoryFilter(null)
                                    viewModel.setSelectedEventTagFilter(null)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAllSelected) EmeraldNeon else TextSecondary
                            )
                        }

                        // Event Mode Filter Chips (Step 8.5)
                        state.allEventTags.forEach { tag ->
                            val isEventSelected = state.selectedEventTagFilter == tag
                            val shape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .clip(shape)
                                    .background(if (isEventSelected) GlowingAmber.copy(alpha = 0.25f) else GlassSlateElevated)
                                    .border(
                                        1.dp,
                                        if (isEventSelected) GlowingAmber else GlowingAmber.copy(alpha = 0.45f),
                                        shape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setSelectedEventTagFilter(if (isEventSelected) null else tag)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isEventSelected) GlowingAmber else TextPrimary
                                )
                            }
                        }

                        // All Active Pulse Categories
                        PulseCategory.entries.filter { !it.isPendingReview }.forEach { cat ->
                            val isSelected = state.selectedCategoryFilter == cat
                            val shape = RoundedCornerShape(10.dp)
                            val isDark = PulseTheme.colors.isDark
                            val activeBg = if (isDark) cat.color.copy(alpha = 0.2f) else EmeraldNeon.copy(alpha = 0.15f)
                            val activeBorder = if (isDark) cat.color else EmeraldNeon
                            val activeContent = if (isDark) cat.color else EmeraldNeon

                            Box(
                                modifier = Modifier
                                    .clip(shape)
                                    .background(if (isSelected) activeBg else GlassSlateElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) activeBorder else BorderSlate,
                                        shape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setCategoryFilter(if (isSelected) null else cat)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) activeContent else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = cat.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) activeContent else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Smart Grouped Transaction Timeline with Sticky Headers
            if (state.groupedTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.selectedCategoryFilter != null) "No transactions for ${state.selectedCategoryFilter?.displayName}" else "No transactions recorded yet.\nMake a UPI payment to start tracking!",
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                state.groupedTransactions.forEach { group ->
                    // Sticky Date Header
                    stickyHeader(key = group.dateLabel) {
                        DateHeader(
                            dateLabel = group.dateLabel,
                            dayTotal = group.dayTotal
                        )
                    }

                    // Transactions for this day
                    items(
                        items = group.transactions,
                        key = { it.id }
                    ) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.openEditBottomSheet(tx)
                            },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 5. Rename & Rule Customization Bottom Sheet
        if (state.editingTransaction != null) {
            EditMerchantBottomSheet(
                transaction = state.editingTransaction,
                onDismiss = { viewModel.closeEditBottomSheet() },
                onSaveRule = { txId, lookupKey, newCategory, newAlias ->
                    viewModel.updateMerchantRuleRetroactively(txId, lookupKey, newCategory, newAlias)
                }
            )
        }



        // 7. Calculator-Style Cash Quick-Add Sheet
        if (isQuickAddOpen) {
            QuickAddSheet(
                onDismiss = { isQuickAddOpen = false },
                onSaveCashExpense = { cashTx ->
                    viewModel.insertTransaction(cashTx)
                    isQuickAddOpen = false
                }
            )
        }



        // 9. 1-Tap Trip / Event Mode Dialog (Step 8.5)
        if (isTripModeOpen) {
            TripModeDialog(
                currentTag = state.activeEventTag,
                onSave = { tag ->
                    viewModel.setActiveEventTag(tag)
                },
                onDismiss = { isTripModeOpen = false }
            )
        }

        // 10. Monthly Variable Budget Dialog (Step 8.5)
        if (isBudgetDialogOpen) {
            BudgetDialog(
                currentBudget = state.monthlyBudget,
                onSave = { budget ->
                    viewModel.setMonthlyBudget(budget)
                },
                onDismiss = { isBudgetDialogOpen = false }
            )
        }
    }
}

/**
 * Segmented Control Button.
 */
@Composable
private fun QuickActionSquircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val isDark = PulseTheme.colors.isDark
    val shape = RoundedCornerShape(18.dp)
    val bg = if (isActive) {
        if (isDark) ElectricLime.copy(alpha = 0.22f) else DeepForestGreen.copy(alpha = 0.12f)
    } else GlassSlateElevated
    val border = if (isActive) {
        if (isDark) ElectricLime else DeepForestGreen
    } else BorderSlate

    Box(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isActive) (if (isDark) ElectricLime else DeepForestGreen) else GlassSlate),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) (if (isDark) Color(0xFF0C120B) else Color.White) else TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Segmented Control Button with full rounded pill geometry and reference selection colors.
 */
@Composable
private fun SegmentButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = PulseTheme.colors.isDark
    val bg = if (isSelected) {
        if (isDark) ElectricLime else DeepForestGreen
    } else Color.Transparent
    val tint = if (isSelected) {
        if (isDark) Color(0xFF0C120B) else Color.White
    } else TextSecondary

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}

/**
 * Sticky Date Group Header.
 */
@Composable
private fun DateHeader(
    dateLabel: String,
    dayTotal: Double
) {
    val indianCurrencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
            maximumFractionDigits = 0
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSlate.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        if (dayTotal > 0) {
            Text(
                text = indianCurrencyFormat.format(dayTotal),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
            )
        }
    }
}

/**
 * Transaction Row Item upgraded with Step 8.5 features:
 * - Net-out display when friend split reimbursements are linked
 * - Commercial refund indicators
 * - Transfer neutral indicators
 * - Trip/Event tag badge
 */
@Composable
private fun TransactionRowItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = remember(transaction.category) {
        PulseCategory.fromString(transaction.category)
    }

    val timeFormatted = remember(transaction.timestamp) {
        SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(transaction.timestamp))
    }

    val isDebit = transaction.type == TransactionType.DEBIT
    val isTransfer = transaction.type == TransactionType.TRANSFER_NEUTRAL
    val isRefund = transaction.isRefund
    val isSplitLinked = isDebit && transaction.netAmount < transaction.amount

    val amountColor = when {
        isTransfer -> TextSecondary
        isRefund -> EmeraldNeon
        isDebit -> CrimsonCoral
        else -> EmeraldNeon
    }
    val amountSign = when {
        isTransfer -> "⇄ "
        isRefund -> "+ "
        isDebit -> "- "
        else -> "+ "
    }
    val displayAmount = if (isSplitLinked) transaction.netAmount else transaction.amount

    PulseGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Circular Category Brand Badge matching Image 2 reference
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(category.color.copy(alpha = 0.16f))
                        .border(1.dp, category.color.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.displayName,
                        tint = category.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Middle: Merchant Name, Time, Verification Micro-badge, Event Tag, Split Tag
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = transaction.displayMerchant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Event Tag Pill
                        if (transaction.eventTag != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GlowingAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = transaction.eventTag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlowingAmber
                                )
                            }
                        }

                        // Refund Badge
                        if (isRefund) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldNeonGlow)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Refund",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldNeon
                                )
                            }
                        }

                        // Transfer Neutral Badge
                        if (isTransfer) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GlassSlateElevated)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Neutral",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$timeFormatted • ${transaction.bankOrAppSource}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Actionable Micro-Badge only when user review is required
                        if (transaction.verificationState == VerificationState.PENDING_USER_TAP) {
                            PendingActionBadge()
                        }
                    }

                    // Split Bill Reimbursed Pill
                    if (isSplitLinked) {
                        Text(
                            text = "🔗 ₹${(transaction.amount - transaction.netAmount).toInt()} reimbursed (Split Net)",
                            fontSize = 10.sp,
                            color = EmeraldNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Right: Formatted Currency Amount (Protected from being squeezed)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = amountSign,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    maxLines = 1
                )
                AnimatedCurrencyText(
                    amount = displayAmount,
                    includeDecimals = true,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        fontFeatureSettings = TABULAR_NUMBERS_FEATURE
                    )
                )
            }
        }
    }
}

/**
 * Actionable micro-badge shown ONLY for unverified transactions requiring user categorization.
 */
@Composable
private fun PendingActionBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(GlowingAmberGlow)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "👆 Tap to Tag",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = GlowingAmber,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * 1-Tap Trip / Event Mode Setup Dialog (Step 8.5)
 */
@Composable
private fun TripModeDialog(
    currentTag: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var tagInput by remember(currentTag) {
        mutableStateOf(currentTag?.removePrefix("#") ?: "")
    }
    val haptic = LocalHapticFeedback.current

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSlateElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🎒", fontSize = 20.sp)
                Text(
                    text = "Trip / Event Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "When active, incoming transactions are automatically tagged so you can track total expenses for a trip or event in 1 tap.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                androidx.compose.material3.OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                    label = { Text("Event Tag (e.g. UjjainTrip)") },
                    prefix = { Text("#", color = EmeraldNeon, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Trip", "Hackathon", "Vacation", "Wedding").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSlate)
                                .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                .clickable {
                                    tagInput = preset
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "#$preset",
                                fontSize = 11.sp,
                                color = EmeraldNeon,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (tagInput.isNotBlank()) {
                        onSave(tagInput.trim())
                    } else {
                        onSave(null)
                    }
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = ObsidianSlate
                )
            ) {
                Text(text = if (tagInput.isNotBlank()) "Start Event Mode" else "Clear Mode", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextMuted)
            }
        }
    )
}

/**
 * Monthly Variable Budget Dialog (Step 8.5 Safe-to-Spend Today)
 */
@Composable
private fun BudgetDialog(
    currentBudget: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var budgetInput by remember(currentBudget) {
        mutableStateOf(currentBudget.toInt().toString())
    }
    val haptic = LocalHapticFeedback.current

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSlateElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🛡️", fontSize = 20.sp)
                Text(
                    text = "Monthly Variable Budget",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Your discretionary spending allowance for the month. SpendLog divides remaining budget by remaining days to calculate Safe-to-Spend Today.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                androidx.compose.material3.OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Monthly Budget") },
                    prefix = { Text("₹", color = EmeraldNeon, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5000, 10000, 20000, 50000).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSlate)
                                .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                .clickable {
                                    budgetInput = preset.toString()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "₹$preset",
                                fontSize = 11.sp,
                                color = EmeraldNeon,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val value = budgetInput.toDoubleOrNull() ?: 10000.0
                    onSave(value)
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = ObsidianSlate
                )
            ) {
                Text(text = "Save Budget", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextMuted)
            }
        }
    )
}

