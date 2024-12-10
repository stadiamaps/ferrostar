package expo.modules.ferrostar.records

import expo.modules.kotlin.types.Enumerable

enum class WaypointKind(val value: String) : Enumerable {
    BREAK("break"),
    VIA("via")
}