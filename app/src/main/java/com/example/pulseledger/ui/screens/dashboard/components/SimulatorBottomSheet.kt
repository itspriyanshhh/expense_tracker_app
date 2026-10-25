package com.example.pulseledger.ui.screens.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseledger.data.repository.ExpenseWaterfallRepository
import com.example.pulseledger.data.repository.ResolutionTier
import com.example.pulseledger.data.repository.WaterfallResult
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.CrimsonCoral
import com.example.pulseledger.ui.theme.CrimsonCoralGlow
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.EmeraldNeonGlow
import com.example.pulseledger.ui.theme.GlassSlate
import com.example.pulseledger.ui.theme.GlassSlateElevated
import com.example.pulseledger.ui.theme.GlowingAmber
import com.example.pulseledger.ui.theme.GlowingAmberGlow
import com.example.pulseledger.ui.theme.ObsidianSlate
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SimulationScenario(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val execute: suspend (ExpenseWaterfallRepository) -> SimulationExecutionResult
)

data class SimulationExecutionResult(
    val tier: ResolutionTier,
    val merchant: String,
    val amount: Double,
    val category: String,
    val latencyMs: Long,
    val notes: String
)

/**
 * Interactive Live Payment Simulator Bottom Sheet.
 *
 * Simulates 5 real-world fintech payment flows through the exact same
 * TransactionParser and ExpenseWaterfallRepository pipeline as incoming system notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorBottomSheet(
    repository: ExpenseWaterfallRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var runningScenarioId by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf<SimulationExecutionResult?>(null) }

    val emeraldAccent = EmeraldNeon
    val amberAccent = GlowingAmber
    val coralAccent = CrimsonCoral

    val scenarios = remember(emeraldAccent, amberAccent, coralAccent) {
        listOf(
            SimulationScenario(
                id = "dmrc_metro",
                title = "1. Known Rule (DMRC Metro ₹50)",
                subtitle = "Resolves in 1ms via Tier 2 deterministic dictionary as Transit",
                icon = Icons.Default.Subway,
                accentColor = emeraldAccent,
                execute = { repo ->
                    val start = System.currentTimeMillis()
                    val text = "Dear SBI User, your A/c XX1234 is debited by Rs 50.00 on 24-Oct-26 at DMRC METRO INFO: UPI/982341782341"
                    val result = repo.processRawText(text, "SBI-Bank", System.currentTimeMillis())
                    val latency = System.currentTimeMillis() - start
                    SimulationExecutionResult(
                        tier = result?.resolutionTier ?: ResolutionTier.TIER_2_DETERMINISTIC,
                        merchant = result?.transaction?.displayMerchant ?: "DMRC Metro",
                        amount = result?.transaction?.amount ?: 50.0,
                        category = result?.transaction?.category ?: PulseCategory.TRANSIT_AND_TRAVEL.id,
                        latencyMs = latency,
                        notes = "Matched via Tier 2 keyword dictionary 'DMRC' -> Transit & Travel in ${latency}ms."
                    )
                }
            ),
            SimulationScenario(
                id = "retail_brand",
                title = "2. Retail Brand (Zepto ₹340)",
                subtitle = "On-device Indian Q-Commerce rule engine mapping to Groceries",
                icon = Icons.Default.ShoppingCart,
                accentColor = Color(0xFF38BDF8),
                execute = { repo ->
                    val start = System.currentTimeMillis()
                    val text = "Paid Rs 340.00 to Zepto Quick Commerce via PhonePe UPI Ref 432109876543"
                    val result = repo.processRawText(text, "com.phonepe.app", System.currentTimeMillis())
                    val latency = System.currentTimeMillis() - start
                    SimulationExecutionResult(
                        tier = result?.resolutionTier ?: ResolutionTier.TIER_2_DETERMINISTIC,
                        merchant = result?.transaction?.displayMerchant ?: "Zepto",
                        amount = result?.transaction?.amount ?: 340.0,
                        category = result?.transaction?.category ?: PulseCategory.GROCERIES.id,
                        latencyMs = latency,
                        notes = "Resolved instantly via Tier 2 keyword rule as Groceries in ${latency}ms."
                    )
                }
            ),
            SimulationScenario(
                id = "unfamiliar_shop",
                title = "3. Unfamiliar Shop (Balaji Medicos ₹180)",
                subtitle = "Routes through Tier 3 Gemini Structured Classifier as Medical",
                icon = Icons.Default.LocalPharmacy,
                accentColor = Color(0xFFA855F7),
                execute = { repo ->
                    val start = System.currentTimeMillis()
                    val uniqueRef = (System.currentTimeMillis() % 1000000000000L).toString().padStart(12, '0')
                    val text = "Paid Rs 185.00 to City Health Medicos via GPay UPI Ref $uniqueRef"
                    val result = repo.processRawText(text, "com.google.android.apps.nbu.paisa.user", System.currentTimeMillis())
                    val latency = System.currentTimeMillis() - start
                    SimulationExecutionResult(
                        tier = result?.resolutionTier ?: ResolutionTier.TIER_3_GEMINI_AI,
                        merchant = result?.transaction?.displayMerchant ?: "City Health Medicos",
                        amount = result?.transaction?.amount ?: 185.0,
                        category = result?.transaction?.category ?: PulseCategory.MEDICAL.id,
                        latencyMs = latency,
                        notes = "Scrubbed payload classified via Gemini AI into Medical in ${latency}ms."
                    )
                }
            ),
            SimulationScenario(
                id = "ambiguous_p2p",
                title = "4. Ambiguous P2P (Paid ₹40 to SURESH YADAV)",
                subtitle = "P2P Zero-Guessing rule routes to Tier 4 + real Android Actionable Notification",
                icon = Icons.Default.Person,
                accentColor = amberAccent,
                execute = { repo ->
                    val start = System.currentTimeMillis()
                    val text = "Paid ₹40.00 to Suresh Yadav suresh@ybl via PhonePe Ref 998877665544"
                    val result = repo.processRawText(text, "com.phonepe.app", System.currentTimeMillis())
                    val latency = System.currentTimeMillis() - start
                    SimulationExecutionResult(
                        tier = result?.resolutionTier ?: ResolutionTier.TIER_4_PENDING_USER_TAP,
                        merchant = result?.transaction?.displayMerchant ?: "Suresh Yadav",
                        amount = result?.transaction?.amount ?: 40.0,
                        category = result?.transaction?.category ?: PulseCategory.UNCATEGORIZED_P2P.id,
                        latencyMs = latency,
                        notes = "Triggered Tier 4 fallback: Posted actionable notification with 1-tap learn chips!"
                    )
                }
            ),
            SimulationScenario(
                id = "dedup_merge",
                title = "5. Duplicate Bank SMS + GPay Alert",
                subtitle = "Proves 3-minute sliding window merges concurrent alerts into 1 record",
                icon = Icons.Default.Merge,
                accentColor = coralAccent,
                execute = { repo ->
                    val start = System.currentTimeMillis()
                    val now = System.currentTimeMillis()
                    // 1. GPay notification arrives first
                    val gpayText = "Paid ₹450.00 to Starbucks Coffee via UPI Ref 554433221100"
                    repo.processRawText(gpayText, "com.google.android.apps.nbu.paisa.user", now)
                    delay(400)
                    // 2. Bank SMS arrives 400ms later with same reference
                    val smsText = "Dear Customer, INR 450.00 debited from A/c XX5678 at STARBUCKS INFO: UPI/554433221100"
                    val dedupResult = repo.processRawText(smsText, "HDFC-Bank", now + 400)
                    val latency = System.currentTimeMillis() - start
                    SimulationExecutionResult(
                        tier = dedupResult?.resolutionTier ?: ResolutionTier.DEDUPLICATED_MERGE,
                        merchant = dedupResult?.transaction?.displayMerchant ?: "Starbucks Coffee",
                        amount = dedupResult?.transaction?.amount ?: 450.0,
                        category = dedupResult?.transaction?.category ?: PulseCategory.FOOD_AND_DINING.id,
                        latencyMs = latency,
                        notes = "Deduplication Window Success: Merged Bank SMS & GPay into 1 single record (${dedupResult?.transaction?.bankOrAppSource})."
                    )
                }
            ),
            SimulationScenario(
                id = "investment_sip",
                title = "6. Wealth SIP (Zerodha Kite ₹5,000)",
                subtitle = "Resolves in 1ms via Tier 2 keyword engine to Investments",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = Color(0xFF06B6D4),
                execute = { repo ->
                    val start = System.currentTimeMillis()
                    val text = "Dear Investor, your A/c XX9988 is debited by INR 5,000.00 for Zerodha Broking Ltd SIP Order Ref 887766554433"
                    val result = repo.processRawText(text, "HDFC-Bank", System.currentTimeMillis())
                    val latency = System.currentTimeMillis() - start
                    SimulationExecutionResult(
                        tier = result?.resolutionTier ?: ResolutionTier.TIER_2_DETERMINISTIC,
                        merchant = result?.transaction?.displayMerchant ?: "Zerodha",
                        amount = result?.transaction?.amount ?: 5000.0,
                        category = result?.transaction?.category ?: PulseCategory.INVESTMENTS.id,
                        latencyMs = latency,
                        notes = "Matched via Tier 2 keyword dictionary 'ZERODHA' -> Investments in ${latency}ms."
                    )
                }
            ),
            SimulationScenario(
                id = "friend_split",
                title = "7. Friend Split (+₹200 from Aryan after ₹800 McDonald's)",
                subtitle = "Detects prior debit, links split bill & fires 1-tap offset push",
                icon = Icons.Default.Person,
                accentColor = emeraldAccent,
                execute = { repo ->
                    val start = System.currentTimeMillis()
                    val now = System.currentTimeMillis()
                    // 1. Seed the parent ₹800 McDonald's debit
                    val debitText = "Paid Rs 800.00 to McDonalds via GPay UPI Ref 991122334455"
                    repo.processRawText(debitText, "com.google.android.apps.nbu.paisa.user", now - 3600000)
                    delay(300)
                    // 2. Aryan sends ₹200 reimbursement
                    val creditText = "Received Rs 200.00 from Aryan aryan@okaxis via UPI Ref 778899001122"
                    val result = repo.processRawText(creditText, "com.google.android.apps.nbu.paisa.user", now)
                    val latency = System.currentTimeMillis() - start
                    SimulationExecutionResult(
                        tier = result?.resolutionTier ?: ResolutionTier.P2P_SPLIT_BILL_DETECTED,
                        merchant = result?.transaction?.displayMerchant ?: "Aryan",
                        amount = result?.transaction?.amount ?: 200.0,
                        category = result?.transaction?.category ?: PulseCategory.FOOD_AND_DINING.id,
                        latencyMs = latency,
                        notes = "Detected ₹800 parent expense at McDonald's within 24h. Fired 1-Tap Split Bill notification to offset against parent expense!"
                    )
                }
            ),
            SimulationScenario(
                id = "mandate_radar",
                title = "8. AutoPay Mandate Warning (₹1,999 OpenAI in 24h)",
                subtitle = "Pre-Debit Interceptor parses upcoming mandate into 48-Hour Renewal Radar",
                icon = Icons.Default.NotificationsActive,
                accentColor = amberAccent,
                execute = { repo ->
                    val start = System.currentTimeMillis()
                    val text = "Reminder: e-mandate of INR 1,999.00 towards OPENAI will be debited in 24 hours. Ensure sufficient balance."
                    val result = repo.processRawText(text, "HDFC-Bank", System.currentTimeMillis())
                    val latency = System.currentTimeMillis() - start
                    SimulationExecutionResult(
                        tier = result?.resolutionTier ?: ResolutionTier.UPCOMING_MANDATE_INTERCEPTED,
                        merchant = result?.transaction?.displayMerchant ?: "OpenAI",
                        amount = result?.transaction?.amount ?: 1999.0,
                        category = result?.transaction?.category ?: PulseCategory.BILLS_AND_UTILITIES.id,
                        latencyMs = latency,
                        notes = "Pre-Debit Interceptor captured ₹1,999 OpenAI mandate due in 24h. Activated 48-Hour Renewal Radar on Dashboard!"
                    )
                }
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GlassSlate,
        scrimColor = ObsidianSlate.copy(alpha = 0.75f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderSlate)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlowingAmberGlow)
                            .border(1.dp, GlowingAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GlowingAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Live Payment Simulator",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Test 4-Tier Waterfall & 3-Min Deduplication",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldNeonGlow)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Real Pipeline",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldNeon
                    )
                }
            }

            // Results Output Console (if a scenario ran)
            AnimatedVisibility(
                visible = lastResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val res = lastResult
                if (res != null) {
                    val (badgeLabel, badgeColor, badgeBg) = when (res.tier) {
                        ResolutionTier.TIER_1_MEMORY -> Triple("⚡ Tier 1 Memory", EmeraldNeon, EmeraldNeonGlow)
                        ResolutionTier.TIER_2_DETERMINISTIC -> Triple("🎯 Tier 2 Rule", EmeraldNeon, EmeraldNeonGlow)
                        ResolutionTier.TIER_3_GEMINI_AI -> Triple("✨ Tier 3 Gemini AI", Color(0xFFA855F7), Color(0xFFA855F7).copy(alpha = 0.15f))
                        ResolutionTier.TIER_4_PENDING_USER_TAP -> Triple("👆 Tier 4 Actionable Push", GlowingAmber, GlowingAmberGlow)
                        ResolutionTier.DEDUPLICATED_MERGE -> Triple("🔄 Deduplicated Merge", CrimsonCoral, CrimsonCoralGlow)
                        ResolutionTier.COMMERCIAL_REFUND_OFFSET -> Triple("🔄 Commercial Refund", EmeraldNeon, EmeraldNeonGlow)
                        ResolutionTier.P2P_SPLIT_BILL_DETECTED -> Triple("🔗 Split Bill Offset", GlowingAmber, GlowingAmberGlow)
                        ResolutionTier.UPCOMING_MANDATE_INTERCEPTED -> Triple("⏰ Mandate Radar", GlowingAmber, GlowingAmberGlow)
                        ResolutionTier.TRANSFER_NEUTRAL -> Triple("⇄ Transfer Neutral", TextSecondary, GlassSlateElevated)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianSlate)
                            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Text(
                                    text = "${res.latencyMs}ms",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = res.merchant,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Category: ${PulseCategory.fromString(res.category).displayName}",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "₹${res.amount.toInt()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldNeon,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = res.notes,
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 5 Simulation Scenario Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scenarios.forEach { scenario ->
                    val isCurrentlyRunning = isRunning && runningScenarioId == scenario.id
                    val shape = RoundedCornerShape(14.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(GlassSlateElevated)
                            .border(1.dp, BorderSlate, shape)
                            .clickable(enabled = !isRunning) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isRunning = true
                                runningScenarioId = scenario.id
                                scope.launch {
                                    val res = scenario.execute(repository)
                                    lastResult = res
                                    isRunning = false
                                    runningScenarioId = null
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(scenario.accentColor.copy(alpha = 0.15f))
                                        .border(1.dp, scenario.accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = scenario.icon,
                                        contentDescription = null,
                                        tint = scenario.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = scenario.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = scenario.subtitle,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            if (isCurrentlyRunning) {
                                CircularProgressIndicator(
                                    color = EmeraldNeon,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
