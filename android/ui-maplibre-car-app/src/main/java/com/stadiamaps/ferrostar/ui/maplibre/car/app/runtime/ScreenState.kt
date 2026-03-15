package com.stadiamaps.ferrostar.ui.maplibre.car.app.runtime

import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.surface.SurfaceGestureCallback
import com.maplibre.compose.surface.rememberMapSurfaceGestureCallback

/**
 * Bridges [SurfaceGestureCallback] events into Compose-observable [MutableState], while
 * forwarding gesture events (scroll, fling, scale) to a map gesture delegate.
 *
 * Usage:
 * 1. Create an instance and assign it to [ComposableScreen.surfaceGestureCallback].
 * 2. Inside a [MapLibreComposable] context, call [rememberSurfaceArea] to wire up map gestures
 *    and observe safe areas in a single call.
 */
class SurfaceAreaTracker : SurfaceGestureCallback {
    val stableArea: MutableState<Rect?> = mutableStateOf(null)
    val visibleArea: MutableState<Rect?> = mutableStateOf(null)

    @Volatile
    var delegate: SurfaceGestureCallback? = null

    override fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea.value = stableArea
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea.value = visibleArea
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        delegate?.onScroll(distanceX, distanceY)
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        delegate?.onFling(velocityX, velocityY)
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        delegate?.onScale(focusX, focusY, scaleFactor)
    }

    /**
     * Wires up map gesture handling (scroll, fling, scale) and returns a [State] tracking the
     * current [SurfaceArea]. Must be called within a [MapLibreComposable] context.
     */
    @Composable
    @MapLibreComposable
    fun rememberSurfaceArea(): State<SurfaceArea?> {
        rememberMapSurfaceGestureCallback { delegate = it }
        return screenSurfaceState(stableArea, visibleArea)
    }
}

data class SurfaceArea(
    val stableArea: Rect,
    val visibleArea: Rect,
    val compositeArea: Rect
)

@Composable
fun screenSurfaceState(tracker: SurfaceAreaTracker): State<SurfaceArea?> =
    screenSurfaceState(tracker.stableArea, tracker.visibleArea)

@Composable
fun screenSurfaceState(
    stableArea: MutableState<Rect?> = remember { mutableStateOf(null) },
    visibleArea: MutableState<Rect?> = remember { mutableStateOf(null) }
): State<SurfaceArea?> {
    return remember(stableArea, visibleArea) {
        derivedStateOf {
            val stable = stableArea.value ?: return@derivedStateOf null
            val visible = visibleArea.value ?: return@derivedStateOf null
            SurfaceArea(
                stableArea = stable,
                visibleArea = visible,
                compositeArea = Rect(stable.left, visible.top, stable.right, visible.bottom)
            )
        }
    }
}

