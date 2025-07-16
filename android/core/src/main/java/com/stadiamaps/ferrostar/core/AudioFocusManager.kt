package com.stadiamaps.ferrostar.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/** An audio focus manager which can be used to duck audio from other apps when speech is active. */
class AudioFocusManager(
    context: Context,
    private val audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
) {
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var audioFocusRequest: AudioFocusRequest? = null

  @Synchronized
  fun requestAudioFocus(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // "Modern" approach (Android 8.0/API 26+)
      val audioAttributes =
          AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()

      val req =
          AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
              .setAudioAttributes(audioAttributes)
              .setOnAudioFocusChangeListener(audioFocusChangeListener)
              .build()
      audioFocusRequest = req

      audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
      // Legacy approach for older Android versions
      // Drop this when we drop support for API level 25.
      audioManager.requestAudioFocus(
          audioFocusChangeListener,
          // Pretty sure this is correct, but don't have an old enough device to verify
          AudioManager.STREAM_MUSIC,
          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) ==
          AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  @Synchronized
  fun releaseAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest?.let { request -> audioManager.abandonAudioFocusRequest(request) }
      audioFocusRequest = null
    } else {
      audioManager.abandonAudioFocus(audioFocusChangeListener)
    }
  }
}
