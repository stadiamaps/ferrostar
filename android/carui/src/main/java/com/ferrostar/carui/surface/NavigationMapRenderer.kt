package com.ferrostar.carui.surface

import android.graphics.Canvas
import android.graphics.Rect
import android.view.Surface
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.ComposeView
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.maplibre.compose.settings.MapControls
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView

class NavigationMapRenderer(
  carContext: CarContext,
  viewModel: NavigationViewModel
) : SurfaceCallback {

  private val composeView = ComposeView(carContext).apply {
    setContent {
      val camera = rememberSaveableMapViewCamera()

      NavigationMapView(
        styleUrl = "https://demotiles.maplibre.org/style.json",
        mapControls = MapControls(),
        camera = camera,
        viewModel = viewModel
      )
    }
  }

  override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
    surfaceContainer.surface?.let { surface ->
      renderComposeView(surface, composeView)
    }
  }

  override fun onVisibleAreaChanged(visibleArea: Rect) {
    super.onVisibleAreaChanged(visibleArea)
  }

  private fun renderComposeView(surface: Surface, composeView: ComposeView) {
    val canvas: Canvas? = surface.lockCanvas(null)
    canvas?.let {
      composeView.layout(0, 0, canvas.width, canvas.height)
      composeView.draw(canvas)
      surface.unlockCanvasAndPost(canvas)
    }
  }
}