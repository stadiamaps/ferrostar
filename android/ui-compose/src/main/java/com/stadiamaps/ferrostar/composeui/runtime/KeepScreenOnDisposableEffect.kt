package com.stadiamaps.ferrostar.composeui.runtime

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * A Composable that keeps the screen on while the hosting Composable is in the view hierarchy. On
 * dispose, the flag is cleared and the screen will return to its normal behavior.
 *
 * See [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON]
 */
@Composable
fun KeepScreenOnDisposableEffect() {
  val window = LocalActivity.current?.window ?: return

  DisposableEffect(Unit) {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
  }
}
