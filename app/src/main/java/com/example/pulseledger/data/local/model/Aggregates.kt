package com.example.pulseledger.data.local.model

/**
 * Aggregated category spend model for Donut Charts and category breakdown.
 */
data class CategorySpend(
    val category: String,
    val totalAmount: Double,
    val transactionCount: Int = 0
)

/**
 * Aggregated daily spend model for Bar Charts (e.g. past 7 days spending curve).
 */
data class DailySpend(
    val dateString: String, // YYYY-MM-DD
    val totalAmount: Double
)
