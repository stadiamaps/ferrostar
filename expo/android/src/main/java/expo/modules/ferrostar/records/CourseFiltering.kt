package expo.modules.ferrostar.records

import expo.modules.kotlin.types.Enumerable

enum class CourseFiltering(val value: String) : Enumerable {
    SNAP_TO_ROUTE("SNAP_TO_ROUTE"),
    RAW("RAW")
}