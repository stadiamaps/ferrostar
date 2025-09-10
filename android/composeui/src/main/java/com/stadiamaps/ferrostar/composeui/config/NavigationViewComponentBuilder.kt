package com.stadiamaps.ferrostar.composeui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.models.CameraControlState
import com.stadiamaps.ferrostar.composeui.theme.DefaultNavigationUITheme
import com.stadiamaps.ferrostar.composeui.theme.NavigationUITheme
import com.stadiamaps.ferrostar.composeui.views.components.CurrentRoadNameView
import com.stadiamaps.ferrostar.composeui.views.components.InstructionsView
import com.stadiamaps.ferrostar.composeui.views.components.TripProgressView
import com.stadiamaps.ferrostar.core.NavigationUiState
import kotlin.time.ExperimentalTime

data class NavigationViewComponentBuilder(
    internal val instructionsView:
        @Composable
        (modifier: Modifier, uiState: NavigationUiState) -> Unit,
    internal val progressView:
        @Composable
        (modifier: Modifier, uiState: NavigationUiState, onTapExit: (() -> Unit)?) -> Unit,
    internal val roadNameView:
        @Composable
        (modifier: Modifier, roadName: String?, cameraControlState: CameraControlState) -> Unit,
    internal val customOverlayView: @Composable (BoxScope.(Modifier) -> Unit)? = null,
    // TODO: We may reasonably be able to add the NavigationMapView here. But not sure how much
    // value that would add
    //    since most of the hard config already exists within the overlay views which are not
    // maplibre specific.
) {
  companion object {
    @OptIn(ExperimentalTime::class)
    fun Default(theme: NavigationUITheme = DefaultNavigationUITheme) =
        NavigationViewComponentBuilder(
            instructionsView = { modifier, uiState ->
              uiState.visualInstruction?.let { instructions ->
                InstructionsView(
                    modifier = modifier,
                    instructions = instructions,
                    theme = theme.instructionRowTheme,
                    remainingSteps = uiState.remainingSteps,
                    distanceToNextManeuver = uiState.progress?.distanceToNextManeuver)
              }
            },
            progressView = { modifier, uiState, onTapExit ->
              uiState.progress?.let { progress ->
                TripProgressView(
                    modifier = modifier,
                    theme = theme.tripProgressViewTheme,
                    progress = progress,
                    onTapExit = onTapExit)
              }
            },
            roadNameView = { modifier, roadName, cameraControlState ->
              if (cameraControlState is CameraControlState.ShowRouteOverview) {
                roadName?.let { roadName ->
                  Row(
                      modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.Bottom,
                      horizontalArrangement = Arrangement.Center) {
                        CurrentRoadNameView(
                            modifier = modifier,
                            theme = theme.roadNameViewTheme,
                            currentRoadName = roadName)

                        Spacer(modifier = Modifier.height(8.dp))
                      }
                }
              }
            })
  }

  fun getCustomOverlayView(): @Composable (BoxScope.(Modifier) -> Unit)? = customOverlayView
}

fun NavigationViewComponentBuilder.withInstructionsView(
    instructionsView: @Composable (modifier: Modifier, uiState: NavigationUiState) -> Unit
): NavigationViewComponentBuilder {
  return copy(instructionsView = instructionsView)
}

fun NavigationViewComponentBuilder.withProgressView(
    progressView:
        @Composable
        (modifier: Modifier, uiState: NavigationUiState, onTapExit: (() -> Unit)?) -> Unit
): NavigationViewComponentBuilder {
  return copy(progressView = progressView)
}

fun NavigationViewComponentBuilder.withRoadNameView(
    roadNameView:
        @Composable
        (modifier: Modifier, roadName: String?, cameraControlState: CameraControlState) -> Unit
): NavigationViewComponentBuilder {
  return copy(roadNameView = roadNameView)
}

fun NavigationViewComponentBuilder.withCustomOverlayView(
    customOverlayView: @Composable (BoxScope.(Modifier) -> Unit)
): NavigationViewComponentBuilder {
  return copy(customOverlayView = customOverlayView)
}
