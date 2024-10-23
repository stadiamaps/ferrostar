package com.stadiamaps.ferrostar.composeui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.theme.DefaultRoadNameViewTheme
import com.stadiamaps.ferrostar.composeui.theme.RoadNameViewTheme

/**
 * A view that displays the estimated arrival time, the remaining distance, and the remaining
 * duration of a trip.
 *
 * @param modifier The modifier to apply to this layout.
 * @param theme The theme to use for this view.
 * @param currentRoadName The name of the current road.
 */
@Composable
fun CurrentRoadNameView(
    currentRoadName: String,
    modifier: Modifier = Modifier,
    theme: RoadNameViewTheme = DefaultRoadNameViewTheme,
) {
  Row(
      modifier =
          modifier
              .shadow(12.dp, shape = RoundedCornerShape(50))
              .background(color = theme.backgroundColor, shape = RoundedCornerShape(50))
              .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
        Text(currentRoadName, style = theme.textStyle)
      }
}

@Preview
@Composable
fun CurrentRoadNameViewPreview() {
  CurrentRoadNameView("Sesame Street")
}
