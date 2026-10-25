package com.example.pulseledger.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import com.example.pulseledger.ui.components.SpendLogBrandIcon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.pulseledger.data.remote.GeminiClassifierClient
import com.example.pulseledger.data.security.KeyStorageProvider
import com.example.pulseledger.data.security.SecureKeyStorage
import com.example.pulseledger.service.SmsBackfillWorker
import com.example.pulseledger.ui.components.PulseGlassCard
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.BorderSlateFocused
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Onboarding and Permission Setup Screen.
 *
 * Implements a 3-step interactive HorizontalPager with live lifecycle permission detection,
 * smooth spring transitions, tactile haptic feedback, and the Dark Fintech Craft aesthetic.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val keyStorage = remember { SecureKeyStorage(context) }

    // Live Permission States
    var isRadarGranted by remember { mutableStateOf(OnboardingHelper.isNotificationListenerGranted(context)) }
    var isShieldActive by remember { mutableStateOf(OnboardingHelper.isIgnoringBatteryOptimizations(context)) }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Step 3 State
    var isBackfillTriggered by remember { mutableStateOf(false) }

    // WorkManager Backfill Observation
    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(SmsBackfillWorker.WORK_NAME)
        .collectAsState(initial = emptyList())

    val backfillWork = workInfos.firstOrNull()
    val isBackfillRunning = backfillWork?.state == WorkInfo.State.RUNNING || isBackfillTriggered
    val isBackfillFinished = backfillWork?.state == WorkInfo.State.SUCCEEDED

    // Lifecycle Observer for Instant ON_RESUME Settings Return Detection
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newRadar = OnboardingHelper.isNotificationListenerGranted(context)
                if (newRadar && !isRadarGranted) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                isRadarGranted = newRadar

                val newShield = OnboardingHelper.isIgnoringBatteryOptimizations(context)
                if (newShield && !isShieldActive) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                isShieldActive = newShield

                val newSms = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                hasSmsPermission = newSms
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // SMS Permission Request Launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isBackfillTriggered = true
            SmsBackfillWorker.enqueue(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F3826),
                        Color(0xFF082015),
                        Color(0xFF04100B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top Status Bar: "01 / 03", segmented bar, and "Skip"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "0${pagerState.currentPage + 1} / 03",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA3D9B8)
                    )

                    // 3-Segment Progress Bar Track
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .height(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0..2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i <= pagerState.currentPage) Color.White else Color(0x33A3D9B8)
                                    )
                            )
                        }
                    }

                    Text(
                        text = "Skip",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA3D9B8),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            OnboardingHelper.setOnboardingCompleted(context)
                            onComplete()
                        }
                    )
                }

                // Large Bold Hero Typography with Glowing Brand Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Money\nBetter Days",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 40.sp,
                        letterSpacing = (-0.5).sp
                    )
                    SpendLogBrandIcon(
                        modifier = Modifier.size(68.dp),
                        animatePulse = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Save, spend, and grow your wealth\n— all in one simple place.",
                    fontSize = 14.sp,
                    color = Color(0xFFA3D9B8),
                    lineHeight = 21.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Step Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> Step1RadarCard(
                        isGranted = isRadarGranted,
                        onGrantClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            OnboardingHelper.openNotificationListenerSettings(context)
                        }
                    )

                    1 -> Step2ShieldCard(
                        isIgnoringOptimizations = isShieldActive,
                        onRequestClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            OnboardingHelper.requestIgnoreBatteryOptimizations(context)
                        }
                    )

                    2 -> Step3AiAndBackfillCard(
                        hasSmsPermission = hasSmsPermission,
                        isBackfillRunning = isBackfillRunning,
                        isBackfillFinished = isBackfillFinished,
                        onRequestSmsBackfill = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (hasSmsPermission) {
                                isBackfillTriggered = true
                                SmsBackfillWorker.enqueue(context)
                            } else {
                                smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage - 1,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage + 1,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        } else {
                            OnboardingHelper.setOnboardingCompleted(context)
                            onComplete()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F3826)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text(
                        text = when (pagerState.currentPage) {
                            0 -> "Get Started →"
                            1 -> "Next: AI & History →"
                            else -> "Launch SpendLog 🚀"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Top Status Indicator Pill with glowing accents.
 */
@Composable
private fun StepIndicatorPill(
    stepNumber: String,
    title: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isCompleted -> EmeraldNeon
            isActive -> GlowingAmber
            else -> BorderSlate
        },
        label = "pill_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> EmeraldNeonGlow
            isActive -> GlowingAmberGlow
            else -> GlassSlate
        },
        label = "pill_bg"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = EmeraldNeon,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Text(
                text = stepNumber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) GlowingAmber else TextMuted
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive || isCompleted) TextPrimary else TextMuted,
            maxLines = 1
        )
    }
}

/**
 * Step 1: Real-Time Payment Radar (Mandatory Notification Listener)
 */
@Composable
private fun Step1RadarCard(
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    PulseGlassCard(
        modifier = Modifier.fillMaxSize(),
        borderColor = if (isGranted) EmeraldNeon.copy(alpha = 0.5f) else GlowingAmber.copy(alpha = 0.4f),
        glowColor = if (isGranted) EmeraldNeonGlow else GlowingAmberGlow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Status Pill
                StatusBannerPill(
                    isComplete = isGranted,
                    completeText = "● Active & Radar Scanning",
                    pendingText = "● Action Required"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hero Icon with Ambient Glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) EmeraldNeonGlow else GlowingAmberGlow)
                        .border(
                            1.5.dp,
                            if (isGranted) EmeraldNeon else GlowingAmber,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Payment Radar",
                        tint = if (isGranted) EmeraldNeon else GlowingAmber,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Real-Time Payment Radar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Detects transactions as they happen from Google Pay, PhonePe, Paytm, BHIM, and bank SMS.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Zero-Login Privacy Callout Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSlateElevated)
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy Shield",
                            tint = EmeraldNeon,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "100% Private & On-Device",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Zero bank logins, zero account credential sharing, and zero OTP access. Data stays in your local Room database.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = onGrantClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) EmeraldNeon else GlowingAmber,
                    contentColor = ObsidianSlate
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGranted) "Notification Access Granted" else "Enable Notification Radar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Step 2: Immortal Background Shield (Battery Optimization Whitelist)
 */
@Composable
private fun Step2ShieldCard(
    isIgnoringOptimizations: Boolean,
    onRequestClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    PulseGlassCard(
        modifier = Modifier.fillMaxSize(),
        borderColor = if (isIgnoringOptimizations) EmeraldNeon.copy(alpha = 0.5f) else GlowingAmber.copy(alpha = 0.4f),
        glowColor = if (isIgnoringOptimizations) EmeraldNeonGlow else GlowingAmberGlow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Status Pill
                StatusBannerPill(
                    isComplete = isIgnoringOptimizations,
                    completeText = "● Shield Active & Unrestricted",
                    pendingText = "● Action Recommended"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hero Icon with Ambient Glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isIgnoringOptimizations) EmeraldNeonGlow else GlowingAmberGlow)
                        .border(
                            1.5.dp,
                            if (isIgnoringOptimizations) EmeraldNeon else GlowingAmber,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Background Shield",
                        tint = if (isIgnoringOptimizations) EmeraldNeon else GlowingAmber,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Immortal Background Shield",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "OEM Android skins aggressively kill background services. Whitelisting battery optimization ensures 100% of your expenses are captured.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // OEM Reliability Guide Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSlateElevated)
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Critical for Vivo / Xiaomi / Samsung / OnePlus",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "FuntouchOS, MIUI/HyperOS, and OneUI aggressively freeze apps. Setting SpendLog to 'Unrestricted' battery keeps your payment watcher alive without draining power.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = onRequestClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIgnoringOptimizations) EmeraldNeon else GlowingAmber,
                    contentColor = ObsidianSlate
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = if (isIgnoringOptimizations) Icons.Default.CheckCircle else Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isIgnoringOptimizations) "Shield Fully Active" else "Disable Battery Optimization",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Step 3: AI Smart Key & 30-Day Instant Backfill (Optional)
 */
@Composable
private fun Step3AiAndBackfillCard(
    hasSmsPermission: Boolean,
    isBackfillRunning: Boolean,
    isBackfillFinished: Boolean,
    onRequestSmsBackfill: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    PulseGlassCard(
        modifier = Modifier.fillMaxSize(),
        borderColor = BorderSlate,
        glowColor = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Engine & Instant Backfill",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BorderSlate)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Optional",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Structured AI classification and 30-day past payments import.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Section 1: Built-in Gemini AI Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSlateElevated)
                    .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google Gemini AI Engine",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        // Built-in status pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldNeon.copy(alpha = 0.15f))
                                .border(1.dp, EmeraldNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Active & Built-in",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldNeon
                            )
                        }
                    }

                    Text(
                        text = "Tier 3 Cloud AI is integrated into the app code. Unfamiliar Indian merchant alerts are structured and classified with zero configuration needed.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )
                }
            }

            // Section 2: 30-Day Instant SMS Backfill Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSlateElevated)
                    .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = GlowingAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "30-Day Historical SMS Backfill",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Scans your SMS inbox for the last 30 days to build your baseline expense charts and auto-train your local vendor memory.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )

                    // Animated Progress Bar when Running
                    if (isBackfillRunning) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Scanning inbox & deduplicating transactions...",
                                fontSize = 12.sp,
                                color = EmeraldNeon
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                color = EmeraldNeon,
                                trackColor = BorderSlate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }

                    // Button
                    Button(
                        onClick = onRequestSmsBackfill,
                        enabled = !isBackfillRunning && !isBackfillFinished,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBackfillFinished) EmeraldNeonGlow else BorderSlateFocused,
                            contentColor = if (isBackfillFinished) EmeraldNeon else TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isBackfillFinished) Icons.Default.CheckCircle else Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isBackfillFinished -> "30-Day History Ingested"
                                isBackfillRunning -> "Importing Transactions..."
                                hasSmsPermission -> "Start 30-Day Scan"
                                else -> "Grant SMS & Scan 30 Days"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable animated status banner pill showing Emerald for Active and Amber for Action Required.
 */
@Composable
private fun StatusBannerPill(
    isComplete: Boolean,
    completeText: String,
    pendingText: String
) {
    val pillBg by animateColorAsState(
        targetValue = if (isComplete) EmeraldNeonGlow else GlowingAmberGlow,
        label = "status_pill_bg"
    )
    val pillBorder by animateColorAsState(
        targetValue = if (isComplete) EmeraldNeon.copy(alpha = 0.6f) else GlowingAmber.copy(alpha = 0.6f),
        label = "status_pill_border"
    )
    val pillTextColor by animateColorAsState(
        targetValue = if (isComplete) EmeraldNeon else GlowingAmber,
        label = "status_pill_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillBg)
            .border(1.dp, pillBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (isComplete) completeText else pendingText,
            color = pillTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}


