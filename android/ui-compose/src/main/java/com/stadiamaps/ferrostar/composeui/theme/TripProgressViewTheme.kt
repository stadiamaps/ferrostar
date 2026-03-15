package com.stadiamaps.ferrostar.composeui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

enum class TripProgressViewStyle {
  /** A simple progress view with only values. */
  SIMPLIFIED,
  /** An progress view with label captions in addition to values. */
  INFORMATIONAL
}

/** Themes for progress view components */
interface TripProgressViewTheme {
  /** The text style for the step distance (or distance to step). */
  @get:Composable val style: TripProgressViewStyle
  /** The text style for the measurement/value. */
  @get:Composable val measurementTextStyle: TextStyle
  /** The text style for the secondary content (label caption). */
  @get:Composable val secondaryTextStyle: TextStyle
  /** The exit button icon color. */
  @get:Composable val exitIconColor: Color
  /** The exit button background color. */
  @get:Composable val exitButtonBackgroundColor: Color
  /** The background color for the view. */
  @get:Composable val backgroundColor: Color
}

object DefaultTripProgressViewTheme : TripProgressViewTheme {
  override val style: TripProgressViewStyle
    @Composable get() = TripProgressViewStyle.SIMPLIFIED

  override val measurementTextStyle: TextStyle
    @Composable
    get() =
        MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)

  override val secondaryTextStyle: TextStyle
    @Composable
    get() =
        MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

  override val exitIconColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSecondary

  override val exitButtonBackgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.secondary

  override val backgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.surface
}
