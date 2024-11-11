package com.stadiamaps.ferrostar.composeui.config

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.theme.FerrostarTheme
import com.stadiamaps.ferrostar.composeui.views.components.CurrentRoadNameView
import com.stadiamaps.ferrostar.composeui.views.components.InstructionsView
import com.stadiamaps.ferrostar.composeui.views.components.TripProgressView
import com.stadiamaps.ferrostar.core.NavigationUiState

data class NavigationViewComponentBuilder(
  val instructionsView: @Composable (modifier: Modifier, uiState: NavigationUiState) -> Unit,
  val progressView: @Composable (modifier: Modifier, uiState: NavigationUiState, onTapExit: (() -> Unit)?) -> Unit,
  val streetNameView: @Composable (modifier: Modifier, roadName: String?) -> Unit,
  // TODO: We may reasonably be able to add the NavigationMapView here. But not sure how much value that would add
  //    since most of the hard config already exists within the overlay views which are not maplibre specific.
) {
  companion object {
    fun Default(theme: FerrostarTheme) = NavigationViewComponentBuilder(
      instructionsView = { modifier, uiState ->
        uiState.visualInstruction?.let { instructions ->
          InstructionsView(
            modifier = modifier,
            instructions = instructions,
            theme = theme.instructionRowTheme,
            remainingSteps = uiState.remainingSteps,
            distanceToNextManeuver = uiState.progress?.distanceToNextManeuver
          )
        }
      },
      progressView = { modifier, uiState, onTapExit ->
        uiState.progress?.let { progress ->
          TripProgressView(
            modifier = modifier,
            theme = theme.tripProgressViewTheme,
            progress = progress,
            onTapExit = onTapExit
          )
        }
      },
      streetNameView = { modifier, roadName ->
        roadName?.let { roadName ->
          CurrentRoadNameView(
            modifier = modifier,
            theme = theme.roadNameViewTheme,
            currentRoadName = roadName
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    )
  }
}