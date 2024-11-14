package com.stadiamaps.ferrostar.composeui.formatting

import android.icu.util.ULocale
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

interface DateTimeFormatter {
  fun format(dateTime: LocalDateTime): String
}

class EstimatedArrivalDateTimeFormatter(
    private var localeOverride: ULocale? = null,
    private val unitStyle: FormatStyle = FormatStyle.SHORT
) : DateTimeFormatter {
  override fun format(dateTime: LocalDateTime): String {
    val locale = localeOverride?.let { Locale(it.language, it.country) } ?: Locale.getDefault()
    val formatter = java.time.format.DateTimeFormatter.ofLocalizedTime(unitStyle).withLocale(locale)
    return formatter.format(dateTime.toJavaLocalDateTime())
  }
}
