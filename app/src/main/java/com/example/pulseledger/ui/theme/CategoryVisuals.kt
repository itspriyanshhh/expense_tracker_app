package com.example.pulseledger.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pulseledger.ui.model.PulseCategory

/**
 * Visual metadata mapper for categories across the app.
 */
object CategoryVisuals {
    fun getColor(category: PulseCategory): Color = category.color

    fun getContainerColor(category: PulseCategory): Color = category.containerColor

    fun getIcon(category: PulseCategory): ImageVector = category.icon

    fun fromName(name: String?): PulseCategory = PulseCategory.fromString(name)
}
