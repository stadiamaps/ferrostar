package com.stadiamaps.ferrostar.ui.shared.icons

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.graphics.drawable.IconCompat
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType

@SuppressLint("DiscouragedApi")
class ManeuverIcon(
    private val context: Context,
    maneuverType: ManeuverType,
    maneuverModifier: ManeuverModifier,
    drivingSide: DrivingSide?,
) {

  private val _identifier: String
  val identifier: String
    get() = _identifier
  private val _resourceId: Int

  init {
    val descriptor =
        listOfNotNull(
            maneuverType.name.replace(" ", "_"),
            maneuverModifier.name.replace(" ", "_")
        )
            .joinToString(separator = "_")

    val hasLeftHandResource = context.resources.getIdentifier(
        "direction_${descriptor}_driving_left".lowercase(),
        "drawable",
        context.packageName) != 0
    if (drivingSide === DrivingSide.LEFT && hasLeftHandResource) {
      this._identifier = "direction_${descriptor}_driving_left".lowercase()
    } else {
      this._identifier = "direction_${descriptor}".lowercase()
    }
    this._resourceId = context.resources.getIdentifier(this.identifier, "drawable", context.packageName)
  }

  val resourceId: Int?
    get() {
      if (_resourceId == 0) {
        return null
      }

      return _resourceId
    }

  fun iconCompat(): IconCompat? =
    resourceId?.let {
      IconCompat.createWithResource(context, it)
    }
}
