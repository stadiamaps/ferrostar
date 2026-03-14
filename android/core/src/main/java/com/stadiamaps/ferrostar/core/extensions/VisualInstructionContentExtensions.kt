package com.stadiamaps.ferrostar.core.extensions

import uniffi.ferrostar.VisualInstructionContent

@Deprecated("Use ui.support.ManeuverIcon")
val VisualInstructionContent.maneuverIconIdentifier: String
  get() {
    val descriptor =
        listOfNotNull(
            maneuverType?.name?.replace(" ", "_"), maneuverModifier?.name?.replace(" ", "_"))
            .joinToString(separator = "_")
    return "direction_${descriptor}".lowercase()
  }
