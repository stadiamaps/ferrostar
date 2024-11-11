package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.DpSize

data class NavigationViewMetrics(
    val progressViewSize: DpSize,
    val instructionsViewSize: DpSize,
) {
    fun mapViewInsets(): PaddingValues {
        return PaddingValues(
            top = instructionsViewSize.height,
            bottom = progressViewSize.height,
        )
    }
}
