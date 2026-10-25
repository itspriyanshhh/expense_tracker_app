package com.example.pulseledger.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.pulseledger.data.local.PulseDatabase
import com.example.pulseledger.data.preference.ThemeMode
import com.example.pulseledger.data.preference.ThemePreferenceManager
import com.example.pulseledger.data.security.SecureKeyStorage
import com.example.pulseledger.service.SmsBackfillWorker
import com.example.pulseledger.ui.components.PulseGlassCard
import com.example.pulseledger.ui.components.SpendLogBrandIcon
import com.example.pulseledger.ui.screens.onboarding.OnboardingHelper
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings & Diagnostics Screen.
 *
 * Provides real-time health checks on:
 * 1. Notification Listener Service
 * 2. Battery Optimization ignore exemption
 * 3. Integrated Gemini 2.5 Structured Classifier AI Engine
 * 4. 3-Minute Sliding Window Deduplication status
 * 5. 30-Day Historical SMS Backfill engine
 * Plus developer tools: simulator launcher, onboarding reset, database clear.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SettingsScreen(
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val keyStorage = remember { SecureKeyStorage(context) }
    val themePreferenceManager = remember { ThemePreferenceManager.getInstance(context) }
    val currentThemeMode by themePreferenceManager.themeMode.collectAsState()

    var isNotificationGranted by remember { mutableStateOf(OnboardingHelper.isNotificationListenerGranted(context)) }
    var isBatteryIgnored by remember { mutableStateOf(OnboardingHelper.isIgnoringBatteryOptimizations(context)) }

    // 30-Day Historical SMS Backfill State
    val backfillPrefs = remember {
        context.getSharedPreferences(SmsBackfillWorker.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isBackfillTriggered by remember { mutableStateOf(false) }
    var lastBackfillTime by remember {
        mutableStateOf(backfillPrefs.getLong(SmsBackfillWorker.KEY_LAST_BACKFILL_TIME, 0L))
    }
    var lastScannedCount by remember {
        mutableStateOf(backfillPrefs.getInt(SmsBackfillWorker.KEY_LAST_SCANNED_COUNT, 0))
    }
    var lastImportedCount by remember {
        mutableStateOf(backfillPrefs.getInt(SmsBackfillWorker.KEY_LAST_IMPORTED_COUNT, 0))
    }

    // WorkManager live backfill observation
    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(SmsBackfillWorker.WORK_NAME)
        .collectAsState(initial = emptyList())

    val backfillWork = workInfos.firstOrNull()
    val isBackfillRunning = backfillWork?.state == WorkInfo.State.RUNNING ||
            (isBackfillTriggered && backfillWork?.state != WorkInfo.State.SUCCEEDED && backfillWork?.state != WorkInfo.State.FAILED)
    val isBackfillFinished = backfillWork?.state == WorkInfo.State.SUCCEEDED

    LaunchedEffect(backfillWork?.state) {
        if (backfillWork?.state == WorkInfo.State.SUCCEEDED) {
            isBackfillTriggered = false
            lastBackfillTime = backfillPrefs.getLong(SmsBackfillWorker.KEY_LAST_BACKFILL_TIME, System.currentTimeMillis())
            lastScannedCount = backfillPrefs.getInt(SmsBackfillWorker.KEY_LAST_SCANNED_COUNT, lastScannedCount)
            lastImportedCount = backfillPrefs.getInt(SmsBackfillWorker.KEY_LAST_IMPORTED_COUNT, lastImportedCount)
            Toast.makeText(context, "30-day historical scan completed!", Toast.LENGTH_SHORT).show()
        } else if (backfillWork?.state == WorkInfo.State.FAILED) {
            isBackfillTriggered = false
        }
    }

    val formattedLastBackfill = remember(lastBackfillTime) {
        if (lastBackfillTime > 0) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(lastBackfillTime))
        } else null
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isBackfillTriggered = true
            SmsBackfillWorker.enqueue(context, force = true)
            Toast.makeText(context, "Scanning past 30 days of bank SMS...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "SMS permission needed to scan history", Toast.LENGTH_SHORT).show()
        }
    }

    val onTriggerBackfill = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!hasSmsPermission) {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        } else {
            isBackfillTriggered = true
            SmsBackfillWorker.enqueue(context, force = true)
            Toast.makeText(context, "Scanning past 30 days of bank SMS...", Toast.LENGTH_SHORT).show()
        }
    }

    var isClearDbDialogOpen by remember { mutableStateOf(false) }
    var isAboutDialogOpen by remember { mutableStateOf(false) }
    var isApiKeyDialogOpen by remember { mutableStateOf(false) }
    var currentStoredKey by remember { mutableStateOf(keyStorage.getGeminiApiKey()) }
    var apiKeyInput by remember { mutableStateOf(keyStorage.getGeminiApiKey() ?: "") }
    val hasApiKey = !currentStoredKey.isNullOrBlank()

    // Re-check permissions when user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationGranted = OnboardingHelper.isNotificationListenerGranted(context)
                isBatteryIgnored = OnboardingHelper.isIgnoringBatteryOptimizations(context)
                hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                lastBackfillTime = backfillPrefs.getLong(SmsBackfillWorker.KEY_LAST_BACKFILL_TIME, 0L)
                lastScannedCount = backfillPrefs.getInt(SmsBackfillWorker.KEY_LAST_SCANNED_COUNT, 0)
                lastImportedCount = backfillPrefs.getInt(SmsBackfillWorker.KEY_LAST_IMPORTED_COUNT, 0)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSlate)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpendLogBrandIcon(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { isAboutDialogOpen = true },
                                animatePulse = false
                            )
                            Text(
                                text = "Settings & Diagnostics",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Engine health, security credentials & system controls",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Section 0: Appearance & Theme
            item {
                Text(
                    text = "APPEARANCE & THEME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                PulseGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldNeonGlow)
                                    .border(1.dp, EmeraldNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (currentThemeMode) {
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    },
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Theme Preference",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when (currentThemeMode) {
                                        ThemeMode.DARK -> "Dark Obsidian Slate active"
                                        ThemeMode.LIGHT -> "Crisp Light Fintech active"
                                        ThemeMode.SYSTEM -> "Following Android System theme"
                                    },
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // 3-Pill Segmented Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSlateElevated)
                                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                val isSelected = currentThemeMode == mode
                                val optionBg by animateColorAsState(
                                    targetValue = if (isSelected) EmeraldNeonGlow else Color.Transparent,
                                    label = "themeOptBg"
                                )
                                val optionBorder by animateColorAsState(
                                    targetValue = if (isSelected) EmeraldNeon.copy(alpha = 0.5f) else Color.Transparent,
                                    label = "themeOptBorder"
                                )
                                val iconTint by animateColorAsState(
                                    targetValue = if (isSelected) EmeraldNeon else TextSecondary,
                                    label = "themeOptIcon"
                                )
                                val textTint by animateColorAsState(
                                    targetValue = if (isSelected) TextPrimary else TextMuted,
                                    label = "themeOptText"
                                )

                                val shape = RoundedCornerShape(9.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shape)
                                        .background(optionBg)
                                        .border(1.dp, optionBorder, shape)
                                        .clickable {
                                            if (!isSelected) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                themePreferenceManager.setThemeMode(mode)
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (mode) {
                                                ThemeMode.DARK -> Icons.Default.DarkMode
                                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                            },
                                            contentDescription = mode.displayName,
                                            tint = iconTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = mode.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textTint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: System Health & Permissions
            item {
                Text(
                    text = "SYSTEM ENGINE HEALTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
            }

            // Card 1: Notification Listener Radar
            item {
                DiagnosticCard(
                    title = "Payment Radar (Notification Listener)",
                    description = if (isNotificationGranted)
                        "Active & listening for UPI, Bank SMS and Wallet alerts."
                    else
                        "Action Required: Listener permission missing. Automatic capture disabled.",
                    isHealthy = isNotificationGranted,
                    healthyLabel = "Active & Guarding",
                    unhealthyLabel = "Action Needed",
                    healthyIcon = Icons.Default.CheckCircle,
                    unhealthyIcon = Icons.Default.Warning,
                    actionText = if (!isNotificationGranted) "Grant Access" else null,
                    onAction = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        OnboardingHelper.openNotificationListenerSettings(context)
                    }
                )
            }

            // Card 2: Battery Optimization Shield
            item {
                DiagnosticCard(
                    title = "Immortal Shield (Battery Exemption)",
                    description = if (isBatteryIgnored)
                        "Exempted from OEM aggressive power cleanup."
                    else
                        "Recommended: Whitelist app to prevent Android background service kills.",
                    isHealthy = isBatteryIgnored,
                    healthyLabel = "Exempted",
                    unhealthyLabel = "Optimized (Fragile)",
                    healthyIcon = Icons.Default.BatteryChargingFull,
                    unhealthyIcon = Icons.Default.BatteryAlert,
                    actionText = if (!isBatteryIgnored) "Request Exemption" else null,
                    onAction = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        OnboardingHelper.requestIgnoreBatteryOptimizations(context)
                    }
                )
            }

            // Card 3: Gemini Cloud AI Classifier (Tier 3)
            item {
                DiagnosticCard(
                    title = "Tier 3 Gemini AI Engine",
                    description = if (hasApiKey)
                        "Google Gemini Cloud AI integrated. Privacy-scrubbed fallback enabled for unfamiliar merchants."
                    else
                        "Optional: Configure your Gemini API key to enable cloud AI categorization for unfamiliar merchants.",
                    isHealthy = hasApiKey,
                    healthyLabel = "Active & Integrated",
                    unhealthyLabel = "Optional / Not Set",
                    healthyIcon = Icons.Default.AutoAwesome,
                    unhealthyIcon = Icons.Default.Key,
                    actionText = if (hasApiKey) "Edit Key" else "Configure",
                    onAction = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        apiKeyInput = keyStorage.getGeminiApiKey() ?: ""
                        isApiKeyDialogOpen = true
                    }
                )
            }

            // Card 4: Deduplication Engine
            item {
                DiagnosticCard(
                    title = "3-Minute Deduplication Window",
                    description = "Sliding 180s buffer prevents double-counting when concurrent Bank SMS & UPI App notifications arrive.",
                    isHealthy = true,
                    healthyLabel = "180s Window Active",
                    unhealthyLabel = "",
                    healthyIcon = Icons.Default.Merge,
                    unhealthyIcon = Icons.Default.Merge,
                    actionText = null,
                    onAction = {}
                )
            }

            // Section 2: Historical SMS Ingestion & Backfill
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "HISTORICAL DATA & SMS INGESTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                HistoricalSmsBackfillCard(
                    hasSmsPermission = hasSmsPermission,
                    isBackfillRunning = isBackfillRunning,
                    isBackfillFinished = isBackfillFinished || lastBackfillTime > 0L,
                    formattedLastBackfill = formattedLastBackfill,
                    lastScannedCount = lastScannedCount,
                    lastImportedCount = lastImportedCount,
                    onTriggerBackfill = onTriggerBackfill
                )
            }

            // Section 3: Data & System Controls
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DATA & SYSTEM CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
            }

            // Action 2: Re-run Onboarding
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSlate)
                        .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenOnboarding()
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassSlateElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Re-Run Onboarding Walkthrough",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Revisit setup guide and permission verification",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Action 3: Clear Database
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSlate)
                        .border(1.dp, CrimsonCoral.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isClearDbDialogOpen = true
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CrimsonCoralGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = CrimsonCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Reset Database Records",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonCoral
                                )
                                Text(
                                    text = "Wipes local transactions and reset merchant memory",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Privacy & Security Architecture
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSlateElevated)
                        .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Zero-Cloud Privacy Architecture",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "• 100% on-device SMS parsing with zero external servers\n" +
                                   "• Zero bank login or netbanking credentials required\n" +
                                   "• Sensitive account suffixes & closing balances are strictly scrubbed\n" +
                                   "• Cloud AI is integrated with privacy payload scrubbing",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Section 4: App About & Brand Hero Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSlateElevated)
                        .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                        .clickable { isAboutDialogOpen = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SpendLogBrandIcon(
                            modifier = Modifier.size(46.dp),
                            animatePulse = true
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SpendLog v1.0.0",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Engineered with 100% on-device privacy • Tap for Details",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // About SpendLog Dialog
        if (isAboutDialogOpen) {
            AlertDialog(
                onDismissRequest = { isAboutDialogOpen = false },
                containerColor = GlassSlate,
                icon = {
                    SpendLogBrandIcon(
                        modifier = Modifier.size(76.dp),
                        animatePulse = true
                    )
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SpendLog",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Version 1.0.0 (Dark Fintech Edition)",
                            fontSize = 12.sp,
                            color = EmeraldNeon
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SpendLog is your intelligent personal finance assistant engineered for the modern Indian UPI and banking ecosystem.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldNeonGlow)
                                .border(1.dp, EmeraldNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🛡️ 100% On-Device Parsing\n⚡ Zero Cloud Leakage Guarantee\n✨ Gemini AI Categorization",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { isAboutDialogOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianSlate)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Gemini API Key Configuration Dialog
        if (isApiKeyDialogOpen) {
            AlertDialog(
                onDismissRequest = { isApiKeyDialogOpen = false },
                containerColor = GlassSlate,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Gemini AI API Key",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "SpendLog uses Gemini Flash for fallback classification of unknown merchants. Raw SMS, bank accounts, and balances are never transmitted.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("Paste AIzaSy... key", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderSlate,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Text(
                            text = "Free API key from ai.google.dev (leave blank to run 100% offline).",
                            fontSize = 11.sp,
                            color = EmeraldNeon.copy(alpha = 0.85f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (apiKeyInput.isNotBlank()) {
                                keyStorage.setGeminiApiKey(apiKeyInput.trim())
                            } else {
                                keyStorage.clearGeminiApiKey()
                            }
                            currentStoredKey = keyStorage.getGeminiApiKey()
                            isApiKeyDialogOpen = false
                            Toast.makeText(context, "API Key updated successfully", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianSlate)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasApiKey) {
                            TextButton(
                                onClick = {
                                    keyStorage.clearGeminiApiKey()
                                    currentStoredKey = null
                                    apiKeyInput = ""
                                    isApiKeyDialogOpen = false
                                    Toast.makeText(context, "API Key cleared", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Clear Key", color = CrimsonCoral)
                            }
                        }
                        TextButton(onClick = { isApiKeyDialogOpen = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                }
            )
        }

        // Clear Database Confirmation Dialog
        if (isClearDbDialogOpen) {
            AlertDialog(
                onDismissRequest = { isClearDbDialogOpen = false },
                containerColor = GlassSlate,
                title = {
                    Text(
                        text = "Reset Database?",
                        fontWeight = FontWeight.Bold,
                        color = CrimsonCoral
                    )
                },
                text = {
                    Text(
                        text = "This will permanently delete all stored transactions and reset learned merchant memory. This action cannot be undone.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val db = PulseDatabase.getInstance(context)
                                db.clearAllTables()
                                withContext(Dispatchers.Main) {
                                    isClearDbDialogOpen = false
                                    Toast.makeText(context, "Database cleared successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonCoral, contentColor = TextPrimary)
                    ) {
                        Text("Yes, Delete All", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isClearDbDialogOpen = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    description: String,
    isHealthy: Boolean,
    healthyLabel: String,
    unhealthyLabel: String,
    healthyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unhealthyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    val shape = RoundedCornerShape(24.dp)
    val badgeColor = if (isHealthy) EmeraldNeon else GlowingAmber
    val badgeBg = if (isHealthy) EmeraldNeonGlow else GlowingAmberGlow

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassSlate)
            .border(1.dp, BorderSlate, shape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isHealthy) healthyIcon else unhealthyIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isHealthy) healthyLabel else unhealthyLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            if (actionText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .clickable(onClick = onAction)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoricalSmsBackfillCard(
    hasSmsPermission: Boolean,
    isBackfillRunning: Boolean,
    isBackfillFinished: Boolean,
    formattedLastBackfill: String?,
    lastScannedCount: Int,
    lastImportedCount: Int,
    onTriggerBackfill: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    PulseGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isBackfillRunning) GlowingAmberGlow else if (isBackfillFinished) EmeraldNeonGlow else GlassSlateElevated
                            )
                            .border(
                                1.dp,
                                if (isBackfillRunning) GlowingAmber.copy(alpha = 0.4f) else if (isBackfillFinished) EmeraldNeon.copy(alpha = 0.35f) else BorderSlate,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = if (isBackfillRunning) GlowingAmber else if (isBackfillFinished) EmeraldNeon else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "30-Day Historical SMS Backfill",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Automated retroactive ledger scan",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Status Badge
                val badgeBg = when {
                    isBackfillRunning -> GlowingAmberGlow
                    isBackfillFinished -> EmeraldNeonGlow
                    !hasSmsPermission -> GlowingAmberGlow
                    else -> GlassSlateElevated
                }
                val badgeColor = when {
                    isBackfillRunning -> GlowingAmber
                    isBackfillFinished -> EmeraldNeon
                    !hasSmsPermission -> GlowingAmber
                    else -> TextSecondary
                }
                val badgeText = when {
                    isBackfillRunning -> "Scanning..."
                    isBackfillFinished -> "✓ Ingested"
                    !hasSmsPermission -> "Action Needed"
                    else -> "Ready"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Text(
                text = "Scans your on-device SMS inbox for the last 30 days to build your baseline expense charts and auto-train your local vendor memory. Runs 100% offline with zero notification spam.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            // Last Scan Statistics Banner
            if (formattedLastBackfill != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSlateElevated)
                        .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Last scan: $formattedLastBackfill • $lastScannedCount scanned • $lastImportedCount parsed",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Animated LinearProgressIndicator when scanning is running
            if (isBackfillRunning) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Scanning inbox & deduplicating transactions...",
                        fontSize = 12.sp,
                        color = EmeraldNeon,
                        fontWeight = FontWeight.SemiBold
                    )
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

            // Action Button
            Button(
                onClick = onTriggerBackfill,
                enabled = !isBackfillRunning,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBackfillFinished && !isBackfillRunning) EmeraldNeonGlow else BorderSlateFocused,
                    contentColor = if (isBackfillFinished && !isBackfillRunning) EmeraldNeon else TextPrimary,
                    disabledContainerColor = GlassSlateElevated,
                    disabledContentColor = TextMuted
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = when {
                        isBackfillRunning -> Icons.Default.CloudSync
                        !hasSmsPermission -> Icons.Default.Security
                        isBackfillFinished -> Icons.Default.Refresh
                        else -> Icons.Default.CloudSync
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isBackfillRunning -> "Importing Transactions..."
                        !hasSmsPermission -> "Grant SMS Access & Scan 30 Days"
                        isBackfillFinished -> "Re-Scan Past 30 Days"
                        else -> "Start 30-Day Scan"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
