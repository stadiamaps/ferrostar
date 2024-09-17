package com.stadiamaps.ferrostar.composeui.runtime

import android.app.Activity
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Get the Window for the current scene (Activity).
 *
 * @return The Window for the current scene, or null if the current context is not an Activity.
 */
@Composable
internal fun window(): Window? {
  val context = LocalContext.current
  return (context as? Activity)?.window ?: return null
}
