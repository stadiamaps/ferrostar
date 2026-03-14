package com.stadiamaps.ferrostar.carapp.template.icons

import android.content.Context
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.stadiamaps.ferrostar.carapp.R

class InterfaceCarIcons(context: Context) {
  val add: CarIcon =
      CarIcon.Builder(IconCompat.createWithResource(context, R.drawable.add_24px)).build()

  val remove: CarIcon =
      CarIcon.Builder(IconCompat.createWithResource(context, R.drawable.remove_24px)).build()

  val volumeMute: CarIcon =
    CarIcon.Builder(IconCompat.createWithResource(context, R.drawable.volume_mute_24px)).build()

  val volumeUp: CarIcon =
      CarIcon.Builder(IconCompat.createWithResource(context, R.drawable.volume_up_24px)).build()

  fun mute(isMuted: Boolean): CarIcon =
    if (isMuted) {
        volumeMute
    } else {
        volumeUp
    }

  val route: CarIcon =
      CarIcon.Builder(IconCompat.createWithResource(context, R.drawable.route_24px)).build()

  val navigation: CarIcon =
      CarIcon.Builder(IconCompat.createWithResource(context, R.drawable.navigation_24px)).build()

  fun camera(isCenteredOnUser: Boolean): CarIcon =
    if (isCenteredOnUser) {
        route
    } else {
        navigation
    }
}
