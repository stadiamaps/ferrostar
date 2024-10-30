package com.stadiamaps.ferrostar.composeui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** Themes for progress view components */
interface RoadNameViewTheme {
  /** The text style for the measurement/value. */
  @get:Composable val textStyle: TextStyle
  /** The background color for the view. */
  @get:Composable val backgroundColor: Color
  /** The border color for the view. */
  @get:Composable val borderColor: Color
}

/**
 * A default theme for the road name view.
 *
 * The text style comes from your material theme. The background properties are hard-coded based on
 * the default polyline styling, as this doesn't have any clear analog in the material theme.
 */
object DefaultRoadNameViewTheme : RoadNameViewTheme {
  override val textStyle: TextStyle
    @Composable
    get() =
        MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.inverseOnSurface)

  override val backgroundColor: Color
    @Composable get() = Color(0x35, 0x83, 0xdd)

  override val borderColor: Color
    @Composable get() = Color.White
}
