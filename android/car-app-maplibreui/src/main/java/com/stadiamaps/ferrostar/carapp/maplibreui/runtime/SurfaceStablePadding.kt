package com.stadiamaps.ferrostar.carapp.maplibreui.runtime

import android.graphics.Rect
import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.models.CameraPadding
import kotlinx.datetime.format.Padding

@Composable
fun surfaceStablePadding(
    stableArea: Rect?,
    additionalPadding: PaddingValues? = null
): PaddingValues {
  val density = LocalDensity.current
  val surfaceSize = LocalWindowInfo.current.containerSize
  val layoutDirection = LocalLayoutDirection.current

  val extraStart = additionalPadding?.calculateStartPadding(layoutDirection) ?: 0.dp
  val extraTop = additionalPadding?.calculateTopPadding() ?: 0.dp
  val extraEnd = additionalPadding?.calculateEndPadding(layoutDirection) ?: 0.dp
  val extraBottom = additionalPadding?.calculateBottomPadding() ?: 0.dp

  val padding =
      if (stableArea != null) {
        with(density) {
          PaddingValues(
              start = stableArea.left.toDp() + extraStart,
              top = stableArea.top.toDp() + extraTop,
              end = (surfaceSize.width - stableArea.right).toDp() + extraEnd,
              bottom = (surfaceSize.height - stableArea.bottom).toDp() + extraBottom
          )
        }
      } else {
        additionalPadding ?: PaddingValues(0.dp)
      }

  return padding
}

@Composable
fun surfaceStableFractionalPadding(
    stableArea: Rect?,
    @FloatRange(from = 0.0, to = 1.0) start: Float = 0.0f,
    @FloatRange(from = 0.0, to = 1.0) top: Float = 0.0f,
    @FloatRange(from = 0.0, to = 1.0) end: Float = 0.0f,
    @FloatRange(from = 0.0, to = 1.0) bottom: Float = 0.0f
): PaddingValues {
  val density = LocalDensity.current
  val surfaceSize = LocalWindowInfo.current.containerSize
  val stableWidth = stableArea?.width() ?: surfaceSize.width
  val stableHeight = stableArea?.height() ?: surfaceSize.height

  val padding =
      if (stableArea != null) {
        with(density) {
          PaddingValues(
              start = (stableArea.left + stableWidth * start).toDp(),
              top = (stableArea.top + stableHeight * top).toDp(),
              end = (surfaceSize.width - stableArea.right + stableWidth * end).toDp(),
              bottom = (surfaceSize.height - stableArea.bottom + stableHeight * bottom).toDp()
          )
        }
      } else {
        PaddingValues(0.dp)
      }

  return padding
}

// CameraPadding

@Composable
fun surfaceStableCameraPadding(
    stableArea: Rect?,
    additionalPadding: PaddingValues? = null
): CameraPadding {
  val padding = surfaceStablePadding(stableArea, additionalPadding)
  return CameraPadding.padding(padding)
}

@Composable
fun surfaceStableFractionalCameraPadding(
    stableArea: Rect?,
    @FloatRange(from = 0.0, to = 1.0) start: Float = 0.0f,
    @FloatRange(from = 0.0, to = 1.0) top: Float = 0.0f,
    @FloatRange(from = 0.0, to = 1.0) end: Float = 0.0f,
    @FloatRange(from = 0.0, to = 1.0) bottom: Float = 0.0f
): CameraPadding {
  val padding = surfaceStableFractionalPadding(
      stableArea,
      start = start,
      top = top,
      end = end,
      bottom = bottom
  )
  return CameraPadding.padding(padding)
}
