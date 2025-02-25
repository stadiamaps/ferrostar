package com.stadiamaps.ferrostar.composeui.views.components.gridviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.R
import com.stadiamaps.ferrostar.composeui.models.CameraControlState
import com.stadiamaps.ferrostar.composeui.views.components.controls.NavigationUIButton
import com.stadiamaps.ferrostar.composeui.views.components.controls.NavigationUIZoomButton
import com.stadiamaps.ferrostar.composeui.views.components.speedlimit.SignageStyle
import com.stadiamaps.ferrostar.composeui.views.components.speedlimit.SpeedLimitView
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit

@Composable
fun NavigatingInnerGridView(
    modifier: Modifier,
    speedLimit: MeasurementSpeed? = null,
    speedLimitStyle: SignageStyle? = null,
    showMute: Boolean = true,
    isMuted: Boolean?,
    onClickMute: () -> Unit = {},
    buttonSize: DpSize,
    cameraControlState: CameraControlState = CameraControlState.Hidden,
    showZoom: Boolean = true,
    onClickZoomIn: () -> Unit = {},
    onClickZoomOut: () -> Unit = {},
    topCenter: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    centerStart: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    bottomCenter: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    bottomEnd: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) }
) {
  InnerGridView(
      modifier,
      topStart = {
        speedLimit?.let {
          speedLimitStyle?.let { style -> SpeedLimitView(speedLimit = it, signageStyle = style) }
        }
      },
      topCenter = topCenter,
      topEnd = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          when (cameraControlState) {
            CameraControlState.Hidden -> {
              // Nothing to draw here :)
            }
            is CameraControlState.ShowRecenter -> {
              // We decided to put this in the bottom corner for now
            }
            is CameraControlState.ShowRouteOverview -> {
              NavigationUIButton(
                  onClick = cameraControlState.updateCamera, buttonSize = buttonSize) {
                    Icon(
                        Icons.Default.Route,
                        modifier = Modifier.rotate(90.0f),
                        contentDescription = stringResource(id = R.string.route_overview))
                  }
            }
          }

          // NOTE: Some controls hidden when the camera is not following the user
          if (showMute &&
              isMuted != null &&
              cameraControlState !is CameraControlState.ShowRecenter) {
            NavigationUIButton(onClick = onClickMute, buttonSize = buttonSize) {
              if (isMuted) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(id = R.string.unmute_description))
              } else {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(id = R.string.mute_description))
              }
            }
          }
        }
      },
      centerStart = centerStart,
      centerEnd = {
        if (showZoom && cameraControlState !is CameraControlState.ShowRecenter) {
          NavigationUIZoomButton(buttonSize, onClickZoomIn, onClickZoomOut)
        }
      },
      bottomStart = {
        if (cameraControlState is CameraControlState.ShowRecenter) {
          NavigationUIButton(onClick = cameraControlState.updateCamera, buttonSize = buttonSize) {
            Icon(
                Icons.Filled.Navigation,
                contentDescription = stringResource(id = R.string.recenter))
          }
        }
      },
      bottomCenter = bottomCenter,
      bottomEnd = bottomEnd)
}

@Preview(device = Devices.PIXEL_5)
@Composable
fun NavigatingInnerGridViewNonTrackingPreview() {
  NavigatingInnerGridView(
      modifier = Modifier.fillMaxSize(),
      speedLimit = MeasurementSpeed(24.6, MeasurementSpeedUnit.MetersPerSecond),
      speedLimitStyle = SignageStyle.MUTCD,
      isMuted = false,
      buttonSize = DpSize(56.dp, 56.dp),
      cameraControlState =
          CameraControlState.ShowRecenter {
            // Do nothing
          })
}

@Preview(device = Devices.PIXEL_5)
@Composable
fun NavigatingInnerGridViewTrackingPreview() {
  NavigatingInnerGridView(
      modifier = Modifier.fillMaxSize(),
      speedLimit = MeasurementSpeed(24.6, MeasurementSpeedUnit.MetersPerSecond),
      speedLimitStyle = SignageStyle.MUTCD,
      isMuted = false,
      buttonSize = DpSize(56.dp, 56.dp),
      cameraControlState =
          CameraControlState.ShowRouteOverview {
            // Do nothing
          })
}

@Preview(
    device =
        "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun NavigatingInnerGridViewLandscapeNonTrackingPreview() {
  NavigatingInnerGridView(
      modifier = Modifier.fillMaxSize(),
      speedLimit = MeasurementSpeed(27.8, MeasurementSpeedUnit.MetersPerSecond),
      speedLimitStyle = SignageStyle.ViennaConvention,
      isMuted = true,
      buttonSize = DpSize(56.dp, 56.dp),
      cameraControlState =
          CameraControlState.ShowRecenter {
            // Do nothing
          })
}

@Preview(
    device =
        "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun NavigatingInnerGridViewLandscapeTrackingPreview() {
  NavigatingInnerGridView(
      modifier = Modifier.fillMaxSize(),
      speedLimit = MeasurementSpeed(27.8, MeasurementSpeedUnit.MetersPerSecond),
      speedLimitStyle = SignageStyle.ViennaConvention,
      isMuted = true,
      buttonSize = DpSize(56.dp, 56.dp),
      cameraControlState =
          CameraControlState.ShowRouteOverview {
            // Do nothing
          })
}
