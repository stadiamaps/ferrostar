package com.stadiamaps.ferrostar.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager

/** See: https://github.com/ChuckerTeam/chucker */
class CustomChuckerInterceptor(private val context: Context) {
  private val chuckerCollector by lazy {
    ChuckerCollector(
        context = context,
        showNotification = true,
        retentionPeriod = RetentionManager.Period.ONE_HOUR,
    )
  }

  private val chuckerInterceptor: ChuckerInterceptor by lazy {
    ChuckerInterceptor.Builder(context)
        .collector(chuckerCollector)
        .maxContentLength(250_000L)
        .redactHeaders("Auth-Token", "Bearer")
        .alwaysReadResponseBody(true)
        .createShortcut(true)
        .build()
  }

  fun build(): ChuckerInterceptor {
    return chuckerInterceptor
  }
}
