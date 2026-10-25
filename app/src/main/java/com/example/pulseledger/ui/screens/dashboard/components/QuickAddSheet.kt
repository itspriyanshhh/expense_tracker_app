package com.example.pulseledger.ui.screens.dashboard.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.ui.theme.BorderSlate
import com.example.pulseledger.ui.theme.CrimsonCoral
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.GlassSlate
import com.example.pulseledger.ui.theme.GlassSlateElevated
import com.example.pulseledger.ui.theme.ObsidianSlate
import com.example.pulseledger.ui.theme.TABULAR_NUMBERS_FEATURE
import com.example.pulseledger.ui.theme.TextMuted
import com.example.pulseledger.ui.theme.TextPrimary
import com.example.pulseledger.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

/**
 * Calculator-Style Cash Quick-Add Bottom Sheet.
 *
 * Triggered by the primary Emerald FAB on the Dashboard for offline cash expenses.
 * Features:
 * - Custom tactile numeric keypad (no soft keyboard pop-up lag).
 * - Inline `+ / -` math support (evaluates e.g. "120 + 35" live).
 * - Category selector carousel.
 * - Instant offline save directly to Room DB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onSaveCashExpense: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val haptic = LocalHapticFeedback.current

    var expression by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PulseCategory.FOOD_AND_DINING) }

    val computedAmount = remember(expression) {
        evaluateExpression(expression)
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
            maximumFractionDigits = 2
        }
    }

    val categories = remember {
        PulseCategory.entries.filter { !it.isPendingReview }
    }

    fun handleKeyPress(key: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when (key) {
            "C" -> expression = ""
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1).trimEnd()
                }
            }
            "+", "-" -> {
                if (expression.isNotEmpty() && !expression.endsWith("+ ") && !expression.endsWith("- ")) {
                    expression = "$expression $key "
                }
            }
            "." -> {
                val lastToken = expression.split(" ").lastOrNull() ?: ""
                if (!lastToken.contains(".")) {
                    expression = if (lastToken.isEmpty()) "${expression}0." else "$expression."
                }
            }
            else -> {
                // Digits 0-9
                val lastToken = expression.split(" ").lastOrNull() ?: ""
                if (lastToken == "0") {
                    expression = expression.dropLast(1) + key
                } else {
                    expression += key
                }
            }
        }
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
                .padding(bottom = 20.dp),
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
                            .background(EmeraldNeon.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Quick Cash Expense",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tactile Offline Calculator",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Amount Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassSlateElevated)
                        .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Cash Out",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonCoral
                    )
                }
            }

            // Big Bold Amount Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSlate)
                    .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (expression.contains("+") || expression.contains("-")) {
                    Text(
                        text = expression,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = if (computedAmount > 0) currencyFormatter.format(computedAmount) else "₹0",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (computedAmount > 0) TextPrimary else TextMuted,
                    style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
                )
            }

            // Note / Merchant Input
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                placeholder = { Text("What did you pay for? (e.g. Chai, Auto, Veggies)", color = TextMuted, fontSize = 13.sp) },
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

            // Category Selector Carousel
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SELECT CATEGORY",
                    fontSize = 11.sp,
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
                                .border(
                                    1.dp,
                                    if (isSelected) cat.color else BorderSlate,
                                    shape
                                )
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

            // Custom Tactile Numeric Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val row1 = listOf("1", "2", "3", "+")
                val row2 = listOf("4", "5", "6", "-")
                val row3 = listOf("7", "8", "9", "C")
                val row4 = listOf(".", "0", "⌫")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row1.forEach { key ->
                        KeypadButton(
                            text = key,
                            isOperator = key == "+",
                            modifier = Modifier.weight(1f),
                            onClick = { handleKeyPress(key) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row2.forEach { key ->
                        KeypadButton(
                            text = key,
                            isOperator = key == "-",
                            modifier = Modifier.weight(1f),
                            onClick = { handleKeyPress(key) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row3.forEach { key ->
                        KeypadButton(
                            text = key,
                            isOperator = key == "C",
                            modifier = Modifier.weight(1f),
                            onClick = { handleKeyPress(key) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row4.forEach { key ->
                        KeypadButton(
                            text = key,
                            isOperator = key == "⌫",
                            modifier = Modifier.weight(if (key == "0") 2f else 1f),
                            onClick = { handleKeyPress(key) }
                        )
                    }
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        if (computedAmount > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val now = System.currentTimeMillis()
                            val cleanMerchant = noteInput.trim().ifBlank { "Cash Expense" }
                            val cashEntity = TransactionEntity(
                                id = "cash_${now}",
                                amount = computedAmount,
                                type = TransactionType.DEBIT,
                                rawMerchant = cleanMerchant,
                                displayMerchant = cleanMerchant,
                                upiVpa = null,
                                upiRefNo = null,
                                bankOrAppSource = "Cash",
                                category = selectedCategory.id,
                                timestamp = now,
                                verificationState = VerificationState.VERIFIED_USER
                            )
                            onSaveCashExpense(cashEntity)
                        }
                    },
                    enabled = computedAmount > 0,
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
                        text = "Save Expense",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    isOperator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val bgColor = if (isOperator) GlassSlateElevated.copy(alpha = 0.8f) else GlassSlateElevated
    val textColor = if (isOperator) EmeraldNeon else TextPrimary
    val borderColor = if (isOperator) EmeraldNeon.copy(alpha = 0.35f) else BorderSlate

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (text == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                style = TextStyle(fontFeatureSettings = TABULAR_NUMBERS_FEATURE)
            )
        }
    }
}

/**
 * Safely parses and evaluates basic expressions like "120 + 35" or "500 - 45".
 */
private fun evaluateExpression(expr: String): Double {
    if (expr.isBlank()) return 0.0
    val tokens = expr.trim().split(" ")
    if (tokens.isEmpty()) return 0.0

    var total = tokens.firstOrNull()?.toDoubleOrNull() ?: 0.0
    var i = 1
    while (i < tokens.size - 1) {
        val op = tokens[i]
        val nextVal = tokens[i + 1].toDoubleOrNull() ?: 0.0
        when (op) {
            "+" -> total += nextVal
            "-" -> total -= nextVal
        }
        i += 2
    }
    return kotlin.math.max(0.0, total)
}
