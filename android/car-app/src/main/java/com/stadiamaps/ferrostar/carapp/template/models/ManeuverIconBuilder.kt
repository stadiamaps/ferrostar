package com.stadiamaps.ferrostar.carapp.template.models

import android.content.Context
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import uniffi.ferrostar.VisualInstructionContent

/**
 * Computes the drawable resource name for this maneuver, matching the convention used by the
 * composeui module's ManeuverImage.
 */
val VisualInstructionContent.maneuverIconName: String
  get() {
    val descriptor =
        listOfNotNull(
                maneuverType?.name?.replace(" ", "_"), maneuverModifier?.name?.replace(" ", "_"))
            .joinToString(separator = "_")
    return "direction_${descriptor}".lowercase()
  }

/**
 * Creates a [CarIcon] from the maneuver drawable resources.
 *
 * The drawable resources must be available in the app's merged resources (e.g., by depending on the
 * composeui module which bundles the direction_*.xml drawables).
 *
 * @param context The context used to resolve drawable resources.
 * @return A [CarIcon] for this maneuver, or null if no matching drawable is found.
 */
fun VisualInstructionContent.toCarIcon(context: Context): CarIcon? {
  val resourceId =
      context.resources.getIdentifier(maneuverIconName, "drawable", context.packageName)
  if (resourceId == 0) return null
  return CarIcon.Builder(IconCompat.createWithResource(context, resourceId)).build()
}
