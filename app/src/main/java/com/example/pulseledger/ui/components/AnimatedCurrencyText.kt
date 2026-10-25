package com.example.pulseledger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pulseledger.ui.theme.CrimsonCoral
import com.example.pulseledger.ui.theme.EmeraldNeon
import com.example.pulseledger.ui.theme.TABULAR_NUMBERS_FEATURE
import com.example.pulseledger.ui.theme.TextPrimary
import java.text.NumberFormat
import java.util.Locale

enum class TransactionDirection {
    EXPENSE,
    INCOME,
    NEUTRAL
}

/**
 * Animated Currency Ticker for Indian Rupee amounts.
 *
 * Guarantees:
 * 1. Strict formatting via NumberFormat.getCurrencyInstance(Locale("en", "IN")).
 * 2. Tabular figures (fontFeatureSettings = "tnum") preventing digit jitter during animations.
 * 3. Smooth animated interpolation whenever the target amount updates.
 */
@Composable
fun AnimatedCurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    direction: TransactionDirection = TransactionDirection.NEUTRAL,
    style: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp
    ),
    textColor: Color? = null,
    showPrefix: Boolean = true,
    includeDecimals: Boolean = true,
    animationDurationMillis: Int = 600
) {
    val animatable = remember { Animatable(amount.toFloat()) }

    LaunchedEffect(amount) {
        animatable.animateTo(
            targetValue = amount.toFloat(),
            animationSpec = tween(
                durationMillis = animationDurationMillis,
                easing = FastOutSlowInEasing
            )
        )
    }

    val currencyFormatter = remember(includeDecimals) {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        if (!includeDecimals) {
            format.maximumFractionDigits = 0
            format.minimumFractionDigits = 0
        } else {
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2
        }
        format
    }

    val currentDisplayValue = animatable.value.toDouble()
    val formattedString = currencyFormatter.format(currentDisplayValue)

    val effectiveColor = textColor
        ?: if (style.color != Color.Unspecified) style.color
        else when (direction) {
            TransactionDirection.EXPENSE -> CrimsonCoral
            TransactionDirection.INCOME -> EmeraldNeon
            TransactionDirection.NEUTRAL -> TextPrimary
        }

    val prefix = if (showPrefix) {
        when (direction) {
            TransactionDirection.EXPENSE -> "- "
            TransactionDirection.INCOME -> "+ "
            TransactionDirection.NEUTRAL -> ""
        }
    } else ""

    // Enforce tabular figures in text style
    val tabularStyle = style.copy(
        color = effectiveColor,
        fontFeatureSettings = TABULAR_NUMBERS_FEATURE
    )

    Text(
        text = "$prefix$formattedString",
        style = tabularStyle,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}

/**
 * Pure format helper utility for static currency strings.
 */
fun formatIndianCurrency(amount: Double, includeDecimals: Boolean = true): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    if (!includeDecimals) {
        format.maximumFractionDigits = 0
        format.minimumFractionDigits = 0
    } else {
        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2
    }
    return format.format(amount)
}
