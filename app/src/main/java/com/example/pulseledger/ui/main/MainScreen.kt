package com.example.pulseledger.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.ui.components.AnimatedCurrencyText
import com.example.pulseledger.ui.components.CategoryBadge
import com.example.pulseledger.ui.components.PulseGlassCard
import com.example.pulseledger.ui.components.QuickCategorizeChip
import com.example.pulseledger.ui.components.TransactionDirection
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.CrimsonCoral
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.GlowingAmber
import com.example.pulseledger.ui.theme.ObsidianSlate
import com.example.pulseledger.ui.theme.PulseTypography
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel(
        factory = MainScreenViewModel.provideFactory(androidx.compose.ui.platform.LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val currentState = state) {
        MainScreenUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(ObsidianSlate),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldNeon)
            }
        }
        is MainScreenUiState.Success -> {
            FintechDashboard(
                dashboardState = currentState.state,
                onResolveTransaction = { txId, lookupKey, category ->
                    viewModel.resolveTransaction(txId, lookupKey, category)
                },
                modifier = modifier
            )
        }
        is MainScreenUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(ObsidianSlate)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "System alert: ${currentState.throwable.message}",
                    color = CrimsonCoral,
                    style = PulseTypography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun FintechDashboard(
    dashboardState: DashboardUiState,
    onResolveTransaction: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<PulseCategory?>(null) }

    val pendingTx = dashboardState.pendingTransactions.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSlate),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SpendLog",
                        style = PulseTypography.displayMedium.copy(fontSize = 24.sp),
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldNeon)
                        )
                        Text(
                            text = "Tier 1 & Tier 2 Active",
                            style = PulseTypography.labelSmall,
                            color = EmeraldNeon
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BorderSlate)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Notifications",
                        tint = TextPrimary
                    )
                }
            }
        }

        // Hero Balance Card
        item {
            PulseGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "TOTAL MONTHLY EXPENSES",
                        style = PulseTypography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedCurrencyText(
                        amount = dashboardState.totalExpenses,
                        direction = TransactionDirection.EXPENSE,
                        style = PulseTypography.displayLarge,
                        includeDecimals = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Monthly Income",
                                style = PulseTypography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AnimatedCurrencyText(
                                amount = dashboardState.totalIncome,
                                direction = TransactionDirection.INCOME,
                                style = PulseTypography.titleMedium,
                                includeDecimals = false
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Savings Rate",
                                style = PulseTypography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val savingsRate = if (dashboardState.totalIncome > 0) {
                                ((dashboardState.totalIncome - dashboardState.totalExpenses) / dashboardState.totalIncome * 100).coerceAtLeast(0.0)
                            } else 0.0
                            Text(
                                text = String.format(Locale.ROOT, "%.1f%%", savingsRate),
                                style = PulseTypography.titleMedium,
                                color = EmeraldNeon
                            )
                        }
                    }
                }
            }
        }

        // Tier 4 1-Tap Actionable Notification Banner (when unverified P2P transaction is pending)
        if (pendingTx != null) {
            item {
                PulseGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = GlowingAmber,
                    glowColor = GlowingAmber
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Pending Review",
                                tint = GlowingAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "P2P Zero-Guessing Review Queue",
                                style = PulseTypography.titleMedium.copy(fontSize = 15.sp),
                                color = GlowingAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${pendingTx.amount.toInt()} to ${pendingTx.displayMerchant} requires 1-tap categorization to memorize vendor rule in Tier 1.",
                            style = PulseTypography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            val candidateCategories = PulseCategory.entries.filter { !it.isPendingReview }
                            candidateCategories.forEach { category ->
                                QuickCategorizeChip(
                                    category = category,
                                    isSelected = false,
                                    onClick = {
                                        val lookup = pendingTx.upiVpa ?: pendingTx.rawMerchant
                                        onResolveTransaction(pendingTx.id, lookup, category.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Text(
                text = "Categories",
                style = PulseTypography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PulseCategory.entries.filter { !it.isPendingReview }.forEach { category ->
                    QuickCategorizeChip(
                        category = category,
                        isSelected = selectedCategoryFilter == category,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == category) null else category
                        }
                    )
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions (${dashboardState.transactions.size})",
                    style = PulseTypography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "3-Min Deduplication OK",
                    style = PulseTypography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Transaction List filtered by category if selected
        val filteredList = if (selectedCategoryFilter == null) {
            dashboardState.transactions
        } else {
            dashboardState.transactions.filter {
                it.category.equals(selectedCategoryFilter!!.id, ignoreCase = true) ||
                it.category.equals(selectedCategoryFilter!!.displayName, ignoreCase = true)
            }
        }

        items(filteredList, key = { it.id }) { txn ->
            val category = PulseCategory.fromString(txn.category)
            val isVerified = txn.verificationState != VerificationState.PENDING_USER_TAP
            val direction = if (txn.type == TransactionType.CREDIT) TransactionDirection.INCOME else TransactionDirection.EXPENSE

            PulseGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(category.containerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.displayName,
                                tint = category.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = txn.displayMerchant,
                                    style = PulseTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isVerified) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Verified: ${txn.verificationState}",
                                        tint = EmeraldNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CategoryBadge(
                                    category = category,
                                    isPending = !isVerified
                                )
                                Text(
                                    text = formatTimestamp(txn.timestamp),
                                    style = PulseTypography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    AnimatedCurrencyText(
                        amount = txn.amount,
                        direction = direction,
                        style = PulseTypography.titleMedium,
                        includeDecimals = true
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val diffMillis = System.currentTimeMillis() - epochMillis
    val minutes = diffMillis / (60 * 1000)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 2 -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.forLanguageTag("en-IN")).format(Date(epochMillis))
    }
}
