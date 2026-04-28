package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.stadiamaps.ferrostar.core.NavigationUiState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import org.maplibre.compose.location.Location
import org.maplibre.spatialk.geojson.Position
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.RouteDeviation

internal val DISPLAY_LOCATION_ANIMATION_DURATION = 1000.milliseconds
private const val DISPLAY_BEARING_LOOKBACK_METERS = 5.0
private const val DISPLAY_BEARING_LOOKAHEAD_METERS = 15.0

@Composable
internal fun rememberDisplayedNavigationLocation(
    uiState: NavigationUiState,
): Location? {
  val userLocation = uiState.location?.toMapLibreLocation() ?: return null
  return rememberRouteSnappedLocation(uiState, userLocation)
}

@Composable
private fun rememberRouteSnappedLocation(
    uiState: NavigationUiState,
    userLocation: Location,
): Location {
  val route =
      remember(uiState.routeGeometry) {
        uiState.routeGeometry?.takeIf { it.size >= 2 }?.let(::RoutePolyline)
      }

  if (!uiState.isNavigating() || uiState.routeDeviation !is RouteDeviation.NoDeviation || route == null) {
    return userLocation
  }

  val targetProjection = remember(route, userLocation.position) { route.project(userLocation.position) }
  val animatedProgress = remember(route) { Animatable(targetProjection.progressMeters, DoubleToVector) }
  val displayedProgress by animatedProgress.asState()

  LaunchedEffect(route, targetProjection.progressMeters) {
    val targetProgress = max(animatedProgress.value, targetProjection.progressMeters)
    if (targetProgress == animatedProgress.value) {
      return@LaunchedEffect
    }

    animatedProgress.animateTo(
        targetValue = targetProgress,
        animationSpec =
            tween(
                durationMillis = DISPLAY_LOCATION_ANIMATION_DURATION.inWholeMilliseconds.toInt(),
                easing = LinearEasing,
            ),
    )
  }

  val displayedPosition = remember(route, displayedProgress) { route.positionAt(displayedProgress) }
  // While on route, use the route tangent as the display bearing so puck and camera rotation stay
  // stable even when course-over-ground is noisy.
  val displayedBearing = remember(route, displayedProgress) { route.bearingAt(displayedProgress) }

  return Location(
      position = displayedPosition,
      accuracy = userLocation.accuracy,
      bearing = displayedBearing,
      bearingAccuracy = userLocation.bearingAccuracy,
      speed = userLocation.speed,
      speedAccuracy = userLocation.speedAccuracy,
      timestamp = TimeSource.Monotonic.markNow(),
  )
}

private class RoutePolyline(
    routeGeometry: List<GeographicCoordinate>,
) {
  private val points = routeGeometry.map { Position(it.lng, it.lat) }
  private val cumulativeDistancesMeters = buildCumulativeDistances(points)
  private val totalLengthMeters = cumulativeDistancesMeters.last()

  fun project(position: Position): RouteProjection {
    var bestDistanceSquared = Double.POSITIVE_INFINITY
    var bestProjection = RouteProjection(progressMeters = 0.0)

    for (index in 0 until points.lastIndex) {
      val segmentProjection = projectOntoSegment(index, position)
      if (segmentProjection.distanceSquaredMeters < bestDistanceSquared) {
        bestDistanceSquared = segmentProjection.distanceSquaredMeters
        bestProjection = RouteProjection(progressMeters = segmentProjection.progressMeters)
      }
    }

    return bestProjection
  }

  fun positionAt(progressMeters: Double): Position {
    val clampedProgress = progressMeters.coerceIn(0.0, totalLengthMeters)
    val segmentIndex = segmentIndexAt(clampedProgress)
    val segmentStart = points[segmentIndex]
    val segmentEnd = points[segmentIndex + 1]
    val segmentLength = cumulativeDistancesMeters[segmentIndex + 1] - cumulativeDistancesMeters[segmentIndex]

    if (segmentLength == 0.0) {
      return segmentStart
    }

    val t = (clampedProgress - cumulativeDistancesMeters[segmentIndex]) / segmentLength
    return Position(
        interpolateCoordinate(segmentStart.longitude, segmentEnd.longitude, t),
        interpolateCoordinate(segmentStart.latitude, segmentEnd.latitude, t),
    )
  }

  fun bearingAt(progressMeters: Double): Double {
    val clampedProgress = progressMeters.coerceIn(0.0, totalLengthMeters)
    val startProgress = (clampedProgress - DISPLAY_BEARING_LOOKBACK_METERS).coerceAtLeast(0.0)
    val endProgress = (clampedProgress + DISPLAY_BEARING_LOOKAHEAD_METERS).coerceAtMost(totalLengthMeters)

    if (endProgress <= startProgress) {
      val segmentIndex = segmentIndexAt(clampedProgress)
      return bearingDegrees(points[segmentIndex], points[segmentIndex + 1])
    }

    return bearingDegrees(positionAt(startProgress), positionAt(endProgress))
  }

  private fun segmentIndexAt(progressMeters: Double): Int {
    for (index in 0 until cumulativeDistancesMeters.lastIndex) {
      if (progressMeters <= cumulativeDistancesMeters[index + 1]) {
        return index
      }
    }
    return max(0, points.lastIndex - 1)
  }

  private fun projectOntoSegment(index: Int, position: Position): SegmentProjection {
    val start = points[index]
    val end = points[index + 1]
    val meanLatitudeRadians = Math.toRadians((start.latitude + end.latitude + position.latitude) / 3.0)
    val scaleX = 111_320.0 * cos(meanLatitudeRadians)
    val scaleY = 111_320.0

    val startX = start.longitude * scaleX
    val startY = start.latitude * scaleY
    val endX = end.longitude * scaleX
    val endY = end.latitude * scaleY
    val pointX = position.longitude * scaleX
    val pointY = position.latitude * scaleY

    val segmentX = endX - startX
    val segmentY = endY - startY
    val segmentLengthSquared = segmentX * segmentX + segmentY * segmentY
    val rawT =
        if (segmentLengthSquared == 0.0) {
          0.0
        } else {
          ((pointX - startX) * segmentX + (pointY - startY) * segmentY) / segmentLengthSquared
        }
    val t = rawT.coerceIn(0.0, 1.0)
    val projectedX = startX + segmentX * t
    val projectedY = startY + segmentY * t
    val distanceSquaredMeters =
        (pointX - projectedX) * (pointX - projectedX) + (pointY - projectedY) * (pointY - projectedY)
    val segmentLengthMeters = sqrt(segmentLengthSquared)

    return SegmentProjection(
        progressMeters = cumulativeDistancesMeters[index] + segmentLengthMeters * t,
        distanceSquaredMeters = distanceSquaredMeters,
    )
  }
}

private data class RouteProjection(
    val progressMeters: Double,
)

private data class SegmentProjection(
    val progressMeters: Double,
    val distanceSquaredMeters: Double,
)

private val DoubleToVector =
    TwoWayConverter<Double, AnimationVector1D>(
        convertToVector = { AnimationVector1D(it.toFloat()) },
        convertFromVector = { it.value.toDouble() },
    )

private fun interpolateCoordinate(start: Double, end: Double, t: Double): Double = start + (end - start) * t

private fun buildCumulativeDistances(points: List<Position>): List<Double> {
  val cumulativeDistances = ArrayList<Double>(points.size)
  cumulativeDistances += 0.0

  for (index in 1 until points.size) {
    cumulativeDistances += cumulativeDistances.last() + distanceMeters(points[index - 1], points[index])
  }

  return cumulativeDistances
}

private fun distanceMeters(a: Position, b: Position): Double {
  val latitudeDeltaMeters = (b.latitude - a.latitude) * 111_320.0
  val averageLatitudeRadians = Math.toRadians((a.latitude + b.latitude) / 2.0)
  val longitudeDeltaMeters = (b.longitude - a.longitude) * 111_320.0 * cos(averageLatitudeRadians)

  return sqrt(
      latitudeDeltaMeters * latitudeDeltaMeters +
          longitudeDeltaMeters * longitudeDeltaMeters,
  )
}

private fun bearingDegrees(start: Position, end: Position): Double {
  val meanLatitudeRadians = Math.toRadians((start.latitude + end.latitude) / 2.0)
  val deltaX = (end.longitude - start.longitude) * cos(meanLatitudeRadians)
  val deltaY = end.latitude - start.latitude
  return (Math.toDegrees(atan2(deltaX, deltaY)) + 360.0) % 360.0
}
