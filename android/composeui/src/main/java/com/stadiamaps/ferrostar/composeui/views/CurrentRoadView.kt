package com.stadiamaps.ferrostar.composeui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.theme.DefaultRoadNameViewTheme
import com.stadiamaps.ferrostar.composeui.theme.RoadNameViewTheme

/**
 * A view that displays the estimated arrival time, the remaining distance, and the remaining
 * duration of a trip.
 *
 * @param currentRoadName The name of the current road.
 * @param modifier The modifier to apply to this layout.
 * @param theme The theme to use for this view.
 * @param borderStroke The stroke to draw for the border (defaults to 1dp using the theme's
 *   borderColor).
 * @param shape The shape of the view (defaults to a 50% rounded corner).
 * @param paddingValues Padding to apply around the name label to increase the shape size (defaults
 *   to 12dp in all directions).
 */
@Composable
fun CurrentRoadNameView(
    currentRoadName: String,
    modifier: Modifier = Modifier,
    theme: RoadNameViewTheme = DefaultRoadNameViewTheme,
    borderStroke: BorderStroke = BorderStroke(1.dp, theme.borderColor),
    shape: Shape = RoundedCornerShape(50),
    paddingValues: PaddingValues = PaddingValues(12.dp),
) {
  Box(
      modifier =
          modifier
              .shadow(12.dp, shape = shape)
              .background(color = theme.backgroundColor, shape = shape)
              .border(borderStroke, shape = shape)
              .padding(paddingValues = paddingValues)) {
        Text(currentRoadName, style = theme.textStyle)
      }
}

@Preview
@Composable
fun CurrentRoadNameViewPreview() {
  CurrentRoadNameView("Sesame Street")
}
