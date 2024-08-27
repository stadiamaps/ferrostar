package com.stadiamaps.ferrostar.maplibreui.runtime

import android.app.Activity
import android.graphics.Color
import android.view.Window
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Get the Window for the current scene (Activity). */
@Composable
fun window(): Window? {
  val context = LocalContext.current
  return (context as? Activity)?.window ?: return null
}

/** Get the WindowInsetsController for the provided window. */
@Composable
fun windowInsetsController(window: Window): WindowInsetsControllerCompat {
  return WindowCompat.getInsetsController(window, window.decorView)
}

/**
 * A Composable effect that automatically hides the system UI (status bar and navigation bar)
 * when the Composable is first composed and restores the system UI when the Composable is
 * disposed.
 */
@Composable
fun AutoHideSystemUIDisposableEffect() {
  val window = window() ?: return
  val insetsController = windowInsetsController(window)
  val colorScheme = MaterialTheme.colorScheme

  DisposableEffect(Unit) {
    // Allow view content to draw behind the status bar.
    WindowCompat.setDecorFitsSystemWindows(window, false)

    window.apply {
      // Make the status bar transparent.
      statusBarColor = Color.TRANSPARENT
    }

    insetsController.apply {
      // Hide the lower navigation bar (grab bar).
      hide(WindowInsetsCompat.Type.navigationBars())
    }

    onDispose {
      // Prevent view content to draw behind the status bar.
      WindowCompat.setDecorFitsSystemWindows(window, true)

      window.apply {
        // Return the status bar color to the primary app/theme color.
        statusBarColor = colorScheme.primary.toArgb()
      }

      insetsController.apply {
        // Show the lower navigation bar (grab bar).
        show(WindowInsetsCompat.Type.navigationBars())
      }
    }
  }
}
