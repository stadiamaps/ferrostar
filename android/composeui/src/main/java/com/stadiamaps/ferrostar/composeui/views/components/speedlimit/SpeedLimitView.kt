package com.stadiamaps.ferrostar.composeui.views.components.speedlimit

import android.content.Context
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stadiamaps.ferrostar.composeui.formatting.MeasurementSpeedFormatter
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit

enum class SignageStyle {
  MUTCD,
  ViennaConvention
}

@Composable
fun SpeedLimitView(
    modifier: Modifier = Modifier,
    speedLimit: MeasurementSpeed,
    signageStyle: SignageStyle = SignageStyle.ViennaConvention, // TODO: This could be nicer
    context: Context = LocalContext.current,
    formatter: MeasurementSpeedFormatter = MeasurementSpeedFormatter(context, speedLimit),
    locale: ULocale = ULocale.getDefault()
) {
  when (signageStyle) {
    SignageStyle.MUTCD ->
        USStyleSpeedLimitView(
            modifier, speedLimit, MeasurementSpeedUnit.MilesPerHour, context, formatter, locale)
    SignageStyle.ViennaConvention ->
        ViennaConventionStyleSpeedLimitView(
            modifier,
            speedLimit,
            MeasurementSpeedUnit.KilometersPerHour,
            context,
            formatter,
            locale)
  }
}
