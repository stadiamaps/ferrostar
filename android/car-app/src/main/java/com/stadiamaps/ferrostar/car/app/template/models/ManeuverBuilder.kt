package com.stadiamaps.ferrostar.car.app.template.models

import android.content.Context
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.Maneuver
import com.stadiamaps.ferrostar.ui.shared.icons.ManeuverIcon
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstructionContent

fun VisualInstructionContent.toCarManeuver(
    context: Context,
    drivingSide: DrivingSide = DrivingSide.RIGHT,
    roundaboutExitNumber: Int? = null
): Maneuver {
  val type = maneuverType.toCarManeuverType(maneuverModifier, drivingSide)

  val maneuverIcon: ManeuverIcon? = if (maneuverType != null && maneuverModifier != null) {
    ManeuverIcon(context, maneuverType!!, maneuverModifier!!)
  } else {
    null
  }

  return Maneuver.Builder(type)
      .apply {
        maneuverIcon?.iconCompat()?.let {
          setIcon(
              CarIcon.Builder(it)
                  .setTint(CarColor.PRIMARY)
                  .build()
          )
        }
        roundaboutExitNumber?.let {
          if (type.isRoundaboutManeuverType()) {
            setRoundaboutExitNumber(it)
          }
        }
      }
      .build()
}

fun ManeuverType?.toCarManeuverType(
    modifier: ManeuverModifier?,
    drivingSide: DrivingSide = DrivingSide.RIGHT
): Int {
  return when (this) {
    ManeuverType.TURN ->
        when (modifier) {
          ManeuverModifier.U_TURN -> Maneuver.TYPE_U_TURN_LEFT
          ManeuverModifier.SHARP_RIGHT -> Maneuver.TYPE_TURN_SHARP_RIGHT
          ManeuverModifier.RIGHT -> Maneuver.TYPE_TURN_NORMAL_RIGHT
          ManeuverModifier.SLIGHT_RIGHT -> Maneuver.TYPE_TURN_SLIGHT_RIGHT
          ManeuverModifier.STRAIGHT -> Maneuver.TYPE_STRAIGHT
          ManeuverModifier.SLIGHT_LEFT -> Maneuver.TYPE_TURN_SLIGHT_LEFT
          ManeuverModifier.LEFT -> Maneuver.TYPE_TURN_NORMAL_LEFT
          ManeuverModifier.SHARP_LEFT -> Maneuver.TYPE_TURN_SHARP_LEFT
          null -> Maneuver.TYPE_UNKNOWN
        }
    ManeuverType.NEW_NAME -> Maneuver.TYPE_NAME_CHANGE
    ManeuverType.DEPART -> Maneuver.TYPE_DEPART
    ManeuverType.ARRIVE -> Maneuver.TYPE_DESTINATION
    ManeuverType.MERGE ->
        when (modifier) {
          ManeuverModifier.SLIGHT_RIGHT,
          ManeuverModifier.RIGHT,
          ManeuverModifier.SHARP_RIGHT -> Maneuver.TYPE_MERGE_RIGHT
          ManeuverModifier.SLIGHT_LEFT,
          ManeuverModifier.LEFT,
          ManeuverModifier.SHARP_LEFT -> Maneuver.TYPE_MERGE_LEFT
          else -> Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED
        }
    ManeuverType.ON_RAMP ->
        when (modifier) {
          ManeuverModifier.SLIGHT_RIGHT,
          ManeuverModifier.RIGHT,
          ManeuverModifier.SHARP_RIGHT -> Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT
          ManeuverModifier.SLIGHT_LEFT,
          ManeuverModifier.LEFT,
          ManeuverModifier.SHARP_LEFT -> Maneuver.TYPE_ON_RAMP_NORMAL_LEFT
          else -> Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT
        }
    ManeuverType.OFF_RAMP ->
        when (modifier) {
          ManeuverModifier.SLIGHT_RIGHT,
          ManeuverModifier.RIGHT,
          ManeuverModifier.SHARP_RIGHT -> Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
          ManeuverModifier.SLIGHT_LEFT,
          ManeuverModifier.LEFT,
          ManeuverModifier.SHARP_LEFT -> Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT
          else -> Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
        }
    ManeuverType.FORK ->
        when (modifier) {
          ManeuverModifier.SLIGHT_RIGHT,
          ManeuverModifier.RIGHT,
          ManeuverModifier.SHARP_RIGHT -> Maneuver.TYPE_FORK_RIGHT
          ManeuverModifier.SLIGHT_LEFT,
          ManeuverModifier.LEFT,
          ManeuverModifier.SHARP_LEFT -> Maneuver.TYPE_FORK_LEFT
          else -> Maneuver.TYPE_FORK_RIGHT
        }
    ManeuverType.END_OF_ROAD ->
        when (modifier) {
          ManeuverModifier.RIGHT,
          ManeuverModifier.SLIGHT_RIGHT,
          ManeuverModifier.SHARP_RIGHT -> Maneuver.TYPE_TURN_NORMAL_RIGHT
          ManeuverModifier.LEFT,
          ManeuverModifier.SLIGHT_LEFT,
          ManeuverModifier.SHARP_LEFT -> Maneuver.TYPE_TURN_NORMAL_LEFT
          else -> Maneuver.TYPE_UNKNOWN
        }
    ManeuverType.CONTINUE -> Maneuver.TYPE_STRAIGHT
    ManeuverType.ROUNDABOUT,
    ManeuverType.ROTARY -> drivingSide.roundaboutEnterAndExit()
    ManeuverType.ROUNDABOUT_TURN -> drivingSide.roundaboutEnterAndExit()
    ManeuverType.EXIT_ROUNDABOUT,
    ManeuverType.EXIT_ROTARY -> drivingSide.roundaboutExit()
    ManeuverType.NOTIFICATION -> Maneuver.TYPE_UNKNOWN
    null -> Maneuver.TYPE_UNKNOWN
  }
}

private fun DrivingSide.roundaboutEnterAndExit(): Int =
    when (this) {
      DrivingSide.RIGHT -> Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW
      DrivingSide.LEFT -> Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW
    }

private fun DrivingSide.roundaboutExit(): Int =
    when (this) {
      DrivingSide.RIGHT -> Maneuver.TYPE_ROUNDABOUT_EXIT_CCW
      DrivingSide.LEFT -> Maneuver.TYPE_ROUNDABOUT_EXIT_CW
    }

fun Int.isRoundaboutManeuverType(): Boolean =
    this == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW ||
        this == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW ||
        this == Maneuver.TYPE_ROUNDABOUT_ENTER_CW ||
        this == Maneuver.TYPE_ROUNDABOUT_ENTER_CCW ||
        this == Maneuver.TYPE_ROUNDABOUT_EXIT_CW ||
        this == Maneuver.TYPE_ROUNDABOUT_EXIT_CCW

