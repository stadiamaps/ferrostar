package com.stadiamaps.ferrostar.carapp.intent

import androidx.car.app.Screen

/**
 * Handles a parsed [NavigationDestination] by deciding which [Screen] to present.
 *
 * Implement this to control what happens when a navigation intent arrives from an external app or
 * voice assistant. Common patterns:
 * - Navigate immediately (return a navigation screen)
 * - Show a confirmation or disclaimer screen first
 * - Show search results or a waypoint picker
 * - Show an alert/warning for unsupported destination types
 *
 * Example:
 * ```
 * val handler = NavigationIntentHandler { destination ->
 *     MyNavigationScreen(carContext, destination)
 * }
 * ```
 */
fun interface NavigationIntentHandler {
  fun handleNavigationIntent(destination: NavigationDestination): Screen
}
