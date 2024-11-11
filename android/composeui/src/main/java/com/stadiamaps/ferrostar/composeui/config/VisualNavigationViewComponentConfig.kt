package com.stadiamaps.ferrostar.composeui.config

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.views.components.CurrentRoadNameView
import com.stadiamaps.ferrostar.composeui.views.components.InstructionsView
import com.stadiamaps.ferrostar.composeui.views.components.TripProgressView
import com.stadiamaps.ferrostar.core.NavigationUiState

data class VisualNavigationViewComponentConfig(
  var instructionsView: @Composable (NavigationUiState) -> Unit,
  var progressView: @Composable (NavigationUiState, onTapExit: (() -> Unit)?) -> Unit,
  var streetNameView: @Composable (String?) -> Unit,
) {
  companion object {
    fun Default() = VisualNavigationViewComponentConfig(
      instructionsView = { uiState ->
        uiState.visualInstruction?.let { instructions ->
          InstructionsView(
            instructions,
            remainingSteps = uiState.remainingSteps,
            distanceToNextManeuver = uiState.progress?.distanceToNextManeuver
          )
        }
      },
      progressView = { uiState, onTapExit ->
        uiState.progress?.let { progress ->
          TripProgressView(progress = progress, onTapExit = onTapExit)
        }
      },
      streetNameView = { roadName ->
        roadName?.let {
          CurrentRoadNameView(it)
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    )
  }
}