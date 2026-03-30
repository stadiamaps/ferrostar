package com.stadiamaps.ferrostar.ui.maplibre.car.app.runtime

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import com.maplibre.compose.surface.SurfaceGestureCallback
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationMapState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal const val DEFAULT_FLING_VELOCITY_FACTOR = 0.1f

@Composable
internal fun defaultFlingDuration(): Duration = 300.milliseconds

internal class ComposeMapSurfaceGestureCallback(
    private val navigationMapState: NavigationMapState,
    private val density: Density,
    private val flingDuration: Duration,
    private val flingVelocityFactor: Float,
) : SurfaceGestureCallback {
  override fun onScroll(distanceX: Float, distanceY: Float) {
    // Preserve the old Ramani surface-gesture sign convention until DHU validation proves
    // that Android Auto's host scroll callbacks need to be inverted here.
    navigationMapState.panBy(
        density.toDpOffset(
            xPx = distanceX,
            yPx = distanceY,
        ))
  }

  override fun onFling(velocityX: Float, velocityY: Float) {
    navigationMapState.flingBy(
        screenDistance =
            density.toDpOffset(
                xPx = -velocityX * flingVelocityFactor,
                yPx = -velocityY * flingVelocityFactor,
            ),
        duration = flingDuration,
    )
  }

  override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
    navigationMapState.scaleBy(scaleFactor)
  }
}

internal fun Density.toDpOffset(xPx: Float, yPx: Float): DpOffset =
    DpOffset(x = xPx.toDp(), y = yPx.toDp())
