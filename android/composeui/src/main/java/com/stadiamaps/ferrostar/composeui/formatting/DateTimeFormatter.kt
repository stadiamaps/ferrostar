package com.stadiamaps.ferrostar.composeui.formatting

import android.icu.util.ULocale
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.util.Locale

interface DateTimeFormatter {
  fun format(dateTime: LocalDateTime): String
}

class EstimatedArrivalDateTimeFormatter(
  private var localeOverride: ULocale? = null,
): DateTimeFormatter {
  override fun format(dateTime: LocalDateTime): String {
    val locale = localeOverride?.let { Locale(it.language, it.country) } ?: Locale.getDefault()
    val formatter =
        java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
          .withLocale(locale)
    return formatter.format(dateTime.toJavaLocalDateTime())
  }
}
