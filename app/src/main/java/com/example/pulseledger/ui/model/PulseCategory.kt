package com.example.pulseledger.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pulseledger.ui.theme.CatBillsUtilities
import com.example.pulseledger.ui.theme.CatEducation
import com.example.pulseledger.ui.theme.CatEntertainment
import com.example.pulseledger.ui.theme.CatFoodDining
import com.example.pulseledger.ui.theme.CatGroceries
import com.example.pulseledger.ui.theme.CatHomeRent
import com.example.pulseledger.ui.theme.CatInsurance
import com.example.pulseledger.ui.theme.CatInvestments
import com.example.pulseledger.ui.theme.CatMedical
import com.example.pulseledger.ui.theme.CatPersonalCare
import com.example.pulseledger.ui.theme.CatShopping
import com.example.pulseledger.ui.theme.CatTransitTravel
import com.example.pulseledger.ui.theme.CatUncategorized

/**
 * Standard expense categories with dark fintech accent colors and icons.
 */
enum class PulseCategory(
    val id: String,
    val displayName: String,
    val color: Color,
    val icon: ImageVector,
    val isPendingReview: Boolean = false
) {
    FOOD_AND_DINING(
        id = "FOOD_AND_DINING",
        displayName = "Food & Dining",
        color = CatFoodDining,
        icon = Icons.Filled.Restaurant
    ),
    GROCERIES(
        id = "GROCERIES",
        displayName = "Groceries",
        color = CatGroceries,
        icon = Icons.Filled.ShoppingCart
    ),
    TRANSIT_AND_TRAVEL(
        id = "TRANSIT_AND_TRAVEL",
        displayName = "Transit & Travel",
        color = CatTransitTravel,
        icon = Icons.Filled.DirectionsTransit
    ),
    SHOPPING(
        id = "SHOPPING",
        displayName = "Shopping",
        color = CatShopping,
        icon = Icons.Filled.ShoppingBag
    ),
    BILLS_AND_UTILITIES(
        id = "BILLS_AND_UTILITIES",
        displayName = "Bills & Utilities",
        color = CatBillsUtilities,
        icon = Icons.Filled.Bolt
    ),
    ENTERTAINMENT(
        id = "ENTERTAINMENT",
        displayName = "Entertainment",
        color = CatEntertainment,
        icon = Icons.Filled.Movie
    ),
    MEDICAL(
        id = "MEDICAL",
        displayName = "Medical",
        color = CatMedical,
        icon = Icons.Filled.LocalHospital
    ),
    EDUCATION(
        id = "EDUCATION",
        displayName = "Education",
        color = CatEducation,
        icon = Icons.Filled.School
    ),
    INVESTMENTS(
        id = "INVESTMENTS",
        displayName = "Investments",
        color = CatInvestments,
        icon = Icons.AutoMirrored.Filled.TrendingUp
    ),
    HOME_AND_RENT(
        id = "HOME_AND_RENT",
        displayName = "Home & Rent",
        color = CatHomeRent,
        icon = Icons.Filled.Home
    ),
    PERSONAL_CARE(
        id = "PERSONAL_CARE",
        displayName = "Personal Care",
        color = CatPersonalCare,
        icon = Icons.Filled.Spa
    ),
    INSURANCE(
        id = "INSURANCE",
        displayName = "Insurance",
        color = CatInsurance,
        icon = Icons.Filled.Shield
    ),
    UNCATEGORIZED_P2P(
        id = "UNCATEGORIZED_P2P",
        displayName = "Uncategorized P2P",
        color = CatUncategorized,
        icon = Icons.Filled.AccountBalanceWallet,
        isPendingReview = true
    );

    /**
     * Alpha container tint suitable for chip backgrounds and badge circles.
     */
    val containerColor: Color
        get() = color.copy(alpha = 0.16f)

    companion object {
        fun fromString(value: String?): PulseCategory {
            if (value == null) return UNCATEGORIZED_P2P
            val normalized = value.trim().uppercase()
            return when {
                normalized == "INVESTMENT" || normalized == "INVESTMENTS" || normalized == "STOCKS" || normalized == "MUTUAL FUNDS" -> INVESTMENTS
                normalized == "RENT" || normalized == "HOUSING" || normalized == "HOME & RENT" || normalized == "HOME_AND_RENT" -> HOME_AND_RENT
                normalized == "PERSONAL CARE" || normalized == "PERSONAL_CARE" || normalized == "WELLNESS" || normalized == "SALON" || normalized == "GYM" -> PERSONAL_CARE
                normalized == "INSURANCE" -> INSURANCE
                else -> entries.find {
                    it.name == normalized ||
                    it.id.equals(normalized, ignoreCase = true) ||
                    it.displayName.equals(value.trim(), ignoreCase = true)
                } ?: UNCATEGORIZED_P2P
            }
        }
    }
}
