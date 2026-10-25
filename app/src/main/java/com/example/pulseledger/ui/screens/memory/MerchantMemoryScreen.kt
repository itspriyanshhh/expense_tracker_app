package com.example.pulseledger.ui.screens.memory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.pulseledger.data.local.entity.MerchantMemoryEntity
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.BorderSlateFocused
import com.example.pulseledger.ui.theme.CrimsonCoral
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.EmeraldNeonGlow
import com.example.pulseledger.ui.theme.GlassSlate
import com.example.pulseledger.ui.theme.GlassSlateElevated
import com.example.pulseledger.ui.theme.GlowingAmber
import com.example.pulseledger.ui.theme.ObsidianSlate
import com.example.pulseledger.ui.theme.TABULAR_NUMBERS_FEATURE
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

/**
 * Merchant Memory Screen (The "Brain" of 100% Accuracy).
 *
 * Tier 1 Memory Vault:
 * - Searchable list of every learned UPI VPA and merchant mapping in Room DB.
 * - Displays payment frequency and cumulative ₹ spent with that vendor.
 * - 1-tap category switcher and custom alias editor.
 * - Manual Pre-Mapping (pre-configure local vendors before paying them).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MerchantMemoryScreen(
    modifier: Modifier = Modifier,
    viewModel: MerchantMemoryViewModel = viewModel(
        factory = MerchantMemoryViewModel.provideFactory(LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
            maximumFractionDigits = 0
        }
    }

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
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldNeonGlow)
                                    .border(1.dp, EmeraldNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Tier 1 Memory Vault",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Deterministic 0ms local payee intelligence",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Pre-map Action Button (Pill shaped matching references)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(com.example.pulseledger.ui.theme.PillPrimary)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.openPreMapping()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Pre-map",
                                tint = com.example.pulseledger.ui.theme.PillOnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Pre-Map",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.pulseledger.ui.theme.PillOnPrimary
                            )
                        }
                    }
                }
            }

            // Stats Bento Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Active Learned Rules Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSlate)
                            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "LEARNED RULES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${state.totalLearnedCount}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldNeonGlow)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "100% Match",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldNeon
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "0ms instant classification",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Total Volume Routed Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSlate)
                            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "VOLUME ROUTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormatter.format(state.totalVolumeSpent),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldNeon,
                                style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Across all learned payees",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search by VPA, alias, or category...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (state.searchQuery.isNotEmpty()) EmeraldNeon else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.setSearchQuery("") }
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GlassSlate,
                        unfocusedContainerColor = GlassSlate,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSlate,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // List of Learned Merchants
            if (state.merchants.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSlate)
                            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(GlassSlateElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = if (state.searchQuery.isEmpty()) "No learned merchants yet" else "No vendors match '${state.searchQuery}'",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (state.searchQuery.isEmpty())
                                    "Tap '+ Pre-Map' or categorize unverified P2P payments to teach Tier 1 rules."
                                else
                                    "Check for typos or try searching by category name.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(state.merchants, key = { it.entity.lookupKey }) { item ->
                    LearnedMerchantCard(
                        item = item,
                        currencyFormatter = currencyFormatter,
                        onEdit = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openEdit(item.entity)
                        },
                        onQuickSwitchCategory = { newCategory ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateMerchantRule(
                                lookupKey = item.entity.lookupKey,
                                newAlias = item.entity.customAlias,
                                newCategory = newCategory.id
                            )
                        }
                    )
                }
            }
        }

        // Pre-Mapping Modal Sheet
        if (state.isPreMappingSheetOpen) {
            PreMapMerchantSheet(
                onDismiss = { viewModel.closePreMapping() },
                onSave = { key, alias, cat ->
                    viewModel.savePreMapping(key, alias, cat)
                }
            )
        }

        // Edit Merchant Rule Modal Sheet
        val editing = state.editingMerchant
        if (editing != null) {
            EditMerchantRuleSheet(
                entity = editing,
                onDismiss = { viewModel.closeEdit() },
                onSave = { key, alias, cat ->
                    viewModel.updateMerchantRule(key, alias, cat)
                },
                onDelete = { key ->
                    viewModel.deleteMerchantRule(key)
                }
            )
        }
    }
}

/**
 * Individual Learned Merchant Card with squircle icon, alias, VPA, count, volume, and quick category chip.
 */
@Composable
private fun LearnedMerchantCard(
    item: LearnedMerchantUiModel,
    currencyFormatter: NumberFormat,
    onEdit: () -> Unit,
    onQuickSwitchCategory: (PulseCategory) -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val entity = item.entity
    val displayName = entity.customAlias ?: entity.lookupKey

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassSlate)
            .border(1.dp, BorderSlate, shape)
            .clickable(onClick = onEdit)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    // Circular Category Badge matching reference images
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(item.category.color.copy(alpha = 0.16f))
                            .border(1.dp, item.category.color.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.category.icon,
                            contentDescription = item.category.displayName,
                            tint = item.category.color,
                            modifier = Modifier.size(20.dp)
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
                                text = displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Tier 1 Verified",
                                tint = EmeraldNeon,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (entity.customAlias != null) {
                            Text(
                                text = entity.lookupKey,
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Learned VPA / Merchant",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Volume & Count (Protected from being squeezed)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (item.totalSpent > 0) currencyFormatter.format(item.totalSpent) else "₹0",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlassSlateElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.transactionCount}x paid",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
                        )
                    }
                }
            }

            // Quick Category Switcher Pill & Edit Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(item.category.color.copy(alpha = 0.12f))
                        .border(1.dp, item.category.color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.category.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = item.category.color
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit rule",
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Edit Rule",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Pre-Mapping Bottom Sheet.
 * Allows adding a local vendor's UPI VPA / merchant string and category *before* ever paying them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreMapMerchantSheet(
    onDismiss: () -> Unit,
    onSave: (lookupKey: String, customAlias: String?, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var lookupKey by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PulseCategory.FOOD_AND_DINING) }

    val categories = remember {
        PulseCategory.entries.filter { !it.isPendingReview }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Pre-Map Vendor Rule",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ensure 100% accuracy before your first payment",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Lookup Key Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "UPI VPA OR MERCHANT STRING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = lookupKey,
                    onValueChange = { lookupKey = it },
                    placeholder = { Text("e.g. sharmastore@paytm or Suresh Auto", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSlate,
                        unfocusedContainerColor = ObsidianSlate,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSlate,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Display Alias Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "FRIENDLY DISPLAY NAME (OPTIONAL)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    placeholder = { Text("e.g. Sharma Ji Grocery or Daily Auto", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSlate,
                        unfocusedContainerColor = ObsidianSlate,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSlate,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Selector Carousel
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ASSIGN CATEGORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val shape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .clip(shape)
                                .background(if (isSelected) cat.color.copy(alpha = 0.2f) else GlassSlateElevated)
                                .border(1.dp, if (isSelected) cat.color else BorderSlate, shape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedCategory = cat
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.displayName,
                                    tint = if (isSelected) cat.color else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = cat.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSlate),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        if (lookupKey.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(lookupKey, alias, selectedCategory.id)
                        }
                    },
                    enabled = lookupKey.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = ObsidianSlate,
                        disabledContainerColor = BorderSlate,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to Tier 1",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Edit Merchant Rule Bottom Sheet.
 * Allows updating alias or switching category, or deleting the rule from Tier 1 Memory.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMerchantRuleSheet(
    entity: MerchantMemoryEntity,
    onDismiss: () -> Unit,
    onSave: (lookupKey: String, customAlias: String?, category: String) -> Unit,
    onDelete: (lookupKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var alias by remember(entity) { mutableStateOf(entity.customAlias ?: "") }
    var selectedCategory by remember(entity) { mutableStateOf(PulseCategory.fromString(entity.category)) }

    val categories = remember {
        PulseCategory.entries.filter { !it.isPendingReview }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                .padding(bottom = 24.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Edit Memory Rule",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = entity.lookupKey,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Delete Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CrimsonCoral.copy(alpha = 0.15f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete(entity.lookupKey)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = CrimsonCoral,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Forget",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonCoral
                        )
                    }
                }
            }

            // Custom Display Alias
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "DISPLAY ALIAS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    placeholder = { Text("Display name (e.g. Daily Chai)", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSlate,
                        unfocusedContainerColor = ObsidianSlate,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSlate,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Selection Carousel
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SWITCH CATEGORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val shape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .clip(shape)
                                .background(if (isSelected) cat.color.copy(alpha = 0.2f) else GlassSlateElevated)
                                .border(1.dp, if (isSelected) cat.color else BorderSlate, shape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedCategory = cat
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.displayName,
                                    tint = if (isSelected) cat.color else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = cat.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSlate),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(entity.lookupKey, alias, selectedCategory.id)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = ObsidianSlate
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Update Rule",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
