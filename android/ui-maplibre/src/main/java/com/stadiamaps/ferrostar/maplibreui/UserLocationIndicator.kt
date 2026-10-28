package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.LocationIndicatorDefaults
import org.maplibre.compose.layers.LocationIndicatorLayer
import org.maplibre.compose.location.LocationMeasurement
import org.maplibre.compose.style.TransitionOptions
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.units.extensions.inMeters

internal const val ACCURACY_CIRCLE_THRESHOLD_METERS = 50.0

/** The outer radius of the dot in the images of [LocationIndicatorDefaults]. */
private val DEFAULT_INDICATOR_DOT_RADIUS = 10.dp

@Composable
@MaplibreComposable
internal fun UserLocationIndicator(
    location: LocationMeasurement?,
    style: NavigationMapPuckStyle,
) {
  val outerRadius = style.dotRadius + style.dotStrokeWidth
  val imageScale = outerRadius / DEFAULT_INDICATOR_DOT_RADIUS
  val dotPainter =
      rememberLocationDotPainter(
          fillColor = style.dotFillColorCurrentLocation,
          strokeColor = style.dotStrokeColor,
          dotRadius = style.dotRadius,
          strokeWidth = style.dotStrokeWidth,
      )

  LocationIndicatorLayer(
      id = "ferrostar-location",
      location = location?.position,
      bearing = location?.course?.takeIf { style.showBearing },
      accuracyRadius =
          location?.horizontalAccuracy?.takeIf { it.inMeters > ACCURACY_CIRCLE_THRESHOLD_METERS },
      bearingAccuracy = location?.courseAccuracy?.takeIf { style.showBearingAccuracy },
      bearingAccuracyColor =
          const(style.bearingColor.copy(alpha = style.bearingColor.alpha * 0.35f)),
      accuracyRadiusColor = style.accuracyFillColor,
      accuracyRadiusBorderColor = style.accuracyStrokeColor,
      topImage = image(dotPainter, size = DpSize(outerRadius * 2, outerRadius * 2)),
      bearingImage =
          if (style.showBearing) LocationIndicatorDefaults.bearingImage(style.bearingColor)
          else null,
      shadowImage = LocationIndicatorDefaults.shadowImage(style.shadowColor),
      bearingImageSize = imageScale,
      shadowImageSize = imageScale,
      // Keeps the indicator in step with the camera, which follows the same location without
      // animation.
      locationTransition = TransitionOptions(duration = Duration.ZERO),
  )
}

@Composable
private fun rememberLocationDotPainter(
    fillColor: Color,
    strokeColor: Color,
    dotRadius: Dp,
    strokeWidth: Dp,
): Painter =
    remember(fillColor, strokeColor, dotRadius, strokeWidth) {
      object : Painter() {
        override val intrinsicSize: Size = Size.Unspecified

        override fun DrawScope.onDraw() {
          drawCircle(color = strokeColor, radius = (dotRadius + strokeWidth).toPx())
          drawCircle(color = fillColor, radius = dotRadius.toPx())
        }
      }
    }
