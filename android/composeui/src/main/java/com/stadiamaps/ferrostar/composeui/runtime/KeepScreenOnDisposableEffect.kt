package com.stadiamaps.ferrostar.composeui.runtime

import android.app.Activity
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
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
 * A Composable that keeps the screen on while the hosting Composable is in the view hierarchy.
 * On dispose, the flag is cleared and the screen will return to its normal behavior.
 *
 * See [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON]
 */
@Composable
fun KeepScreenOnDisposableEffect() {
  val window = window() ?: return

  DisposableEffect(Unit) {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
  }
}
