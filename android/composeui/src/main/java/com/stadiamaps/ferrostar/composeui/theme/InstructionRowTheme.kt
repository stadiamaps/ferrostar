package com.stadiamaps.ferrostar.composeui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** Themes for instruction banner view components */
interface InstructionRowTheme {
  /** The text style for the step distance (or distance to step). */
  @get:Composable val distanceTextStyle: TextStyle
  /** The style for instruction text. */
  @get:Composable val instructionTextStyle: TextStyle
  /** The color of the icon. */
  @get:Composable val iconTintColor: Color
  /** The background color for the view. */
  @get:Composable val backgroundColor: Color
}

/** Default theme using the material theme. */
object DefaultInstructionRowTheme : InstructionRowTheme {
  override val distanceTextStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.titleLarge.merge(color = MaterialTheme.colorScheme.onSurface)

  override val instructionTextStyle: TextStyle
    @Composable
    get() =
        MaterialTheme.typography.headlineSmall.merge(
            color = MaterialTheme.colorScheme.onSurfaceVariant)

  override val iconTintColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

  override val backgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
}
