package com.stadiamaps.ferrostar.composeui.formatting

import android.icu.util.ULocale
import java.util.Locale
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.FormatStyle

interface DateTimeFormatter {
  fun format(dateTime: LocalDateTime): String
}

class EstimatedArrivalDateTimeFormatter(
    private var locale: ULocale = ULocale.getDefault(),
    private val unitStyle: FormatStyle = FormatStyle.LONG
) : DateTimeFormatter {
  override fun format(dateTime: LocalDateTime): String {
    val locale = locale.let { Locale(it.language, it.country) }
    val formatter =
        java.time.format.DateTimeFormatter.ofLocalizedTime(unitStyle)
            .withLocale(locale)
    return formatter.format(dateTime.toJavaLocalDateTime())
  }
}
