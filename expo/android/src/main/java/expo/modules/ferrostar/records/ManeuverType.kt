package expo.modules.ferrostar.records

import expo.modules.kotlin.types.Enumerable

enum class ManeuverType(val value: String) : Enumerable {
    TURN("turn"),
    NEW_NAME("new_name"),
    DEPART("depart"),
    ARRIVE("arrive"),
    MERGE("merge"),
    ON_RAMP("on_ramp"),
    OFF_RAMP("off_ramp"),
    FORK("fork"),
    END_OF_ROAD("end_of_road"),
    CONTINUE("continue"),
    ROUNDABOUT("roundabout"),
    ROTARY("rotary"),
    ROUNDABOUT_TURN("roundabout_turn"),
    NOTIFICATION("notification"),
    EXIT_ROUNDABOUT("exit_roundabout"),
    EXIT_ROTARY("exit_rotary")
}