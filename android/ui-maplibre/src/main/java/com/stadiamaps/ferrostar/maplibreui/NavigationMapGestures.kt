package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.ui.unit.DpOffset
import uniffi.ferrostar.GeographicCoordinate

enum class NavigationMapClickResult {
  Pass,
  Consume,
}

typealias NavigationMapClickHandler = (GeographicCoordinate, DpOffset) -> NavigationMapClickResult
