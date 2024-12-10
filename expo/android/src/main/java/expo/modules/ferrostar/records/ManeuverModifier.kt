package expo.modules.ferrostar.records

import expo.modules.kotlin.types.Enumerable

enum class ManeuverModifier(val value: String) : Enumerable {
    U_TURN("u_turn"),
    SHARP_RIGHT("sharp_right"),
    RIGHT("right"),
    SLIGHT_RIGHT("slight_right"),
    STRAIGHT("straight"),
    SLIGHT_LEFT("slight_left"),
    LEFT("left"),
    SHARP_LEFT("sharp_left")
}