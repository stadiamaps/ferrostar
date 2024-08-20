package com.stadiamaps.ferrostar.composeui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

enum class ArrivalViewStyle {
  /** A simple arrival view with only values. */
  SIMPLIFIED,
  /** An arrival view with label captions in addition to values. */
  INFORMATIONAL
}

/** Themes for arrival view components */
interface ArrivalViewTheme {
  /** The text style for the step distance (or distance to step). */
  @get:Composable val style: ArrivalViewStyle
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

object DefaultArrivalViewTheme : ArrivalViewTheme {
  override val style: ArrivalViewStyle
    @Composable get() = ArrivalViewStyle.SIMPLIFIED

  override val measurementTextStyle: TextStyle
    @Composable
    get() =
        MaterialTheme.typography.titleLarge.copy(
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
