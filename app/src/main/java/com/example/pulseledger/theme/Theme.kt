package com.example.pulseledger.theme

import androidx.compose.runtime.Composable

@Composable
fun PulseLedgerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    com.example.pulseledger.ui.theme.PulseLedgerTheme(
        darkTheme = darkTheme,
        content = content
    )
}
