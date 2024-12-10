package expo.modules.ferrostar.records

import expo.modules.kotlin.types.Enumerable

enum class LocationMode(val value: String) : Enumerable {
    FUSED("fused"),
    DEFAULT("default"),
    SIMULATED("simulated")
}