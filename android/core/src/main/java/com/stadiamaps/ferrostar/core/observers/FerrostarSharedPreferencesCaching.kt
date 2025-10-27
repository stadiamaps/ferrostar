package com.stadiamaps.ferrostar.core.observers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import uniffi.ferrostar.NavigationCache

/**
 * A SharedPreferences-based implementation of NavigationCache for Android. This provides persistent
 * storage for navigation data across app sessions.
 *
 * @param context The Android context used to access SharedPreferences
 * @param preferencesName The name of the SharedPreferences file (defaults to "ferrostar_cache")
 * @param key The key used to store the navigation data (defaults to "ferrostar_navigation_data")
 */
class FerrostarSharedPreferencesCaching(
    private val context: Context,
    private val preferencesName: String = "ferrostar_cache",
    private val key: String = "ferrostar_navigation_data"
) : NavigationCache {

  private val sharedPreferences: SharedPreferences by lazy {
    context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
  }

  /**
   * Saves navigation data to SharedPreferences. The ByteArray is converted to a Base64 string for
   * storage.
   *
   * @param record The navigation data to save as a ByteArray
   */
  override fun save(record: ByteArray) {
    val encodedData = android.util.Base64.encodeToString(record, android.util.Base64.DEFAULT)
    sharedPreferences.edit { putString(key, encodedData) }
  }

  /**
   * Loads navigation data from SharedPreferences. Returns null if no data is found or if decoding
   * fails.
   *
   * @return The cached navigation data as a ByteArray, or null if not found
   */
  override fun load(): ByteArray? {
    return try {
      val encodedData = sharedPreferences.getString(key, null) ?: return null
      android.util.Base64.decode(encodedData, android.util.Base64.DEFAULT)
    } catch (e: IllegalArgumentException) {
      // Base64 decoding failed, return null
      null
    }
  }

  /** Deletes the cached navigation data from SharedPreferences. */
  override fun delete() {
    sharedPreferences.edit { remove(key) }
  }
}
