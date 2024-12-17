package com.stadiamaps.ferrostar.composeui.views.components.speedlimit

import android.content.Context
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stadiamaps.ferrostar.composeui.formatting.MeasurementSpeedFormatter
import com.stadiamaps.ferrostar.composeui.measurement.localizedString
import com.stadiamaps.ferrostar.composeui.support.GreenScreenPreview
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit

@Composable
fun ViennaConventionStyleSpeedLimitView(
    modifier: Modifier = Modifier,
    speedLimit: MeasurementSpeed,
    units: MeasurementSpeedUnit = MeasurementSpeedUnit.KilometersPerHour,
    context: Context = LocalContext.current,
    formatter: MeasurementSpeedFormatter = MeasurementSpeedFormatter(context, speedLimit),
    locale: ULocale = ULocale.getDefault()
) {
  val formattedSpeed = formatter.formattedValue(locale, units)

  Box(
      modifier =
          modifier
              .height(64.dp)
              .width(64.dp)
              .background(color = Color.Red, shape = RoundedCornerShape(50))
              .padding(6.dp)) {
        Box(
            modifier =
                Modifier.height(56.dp)
                    .width(56.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(50))
                    .padding(4.dp)) {
              Column(
                  modifier = Modifier.fillMaxSize(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
                    Text(
                        text = formattedSpeed,
                        fontSize =
                            when {
                              formattedSpeed.length > 3 -> 14.sp
                              formattedSpeed.length > 2 -> 18.sp
                              else -> 24.sp
                            },
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight =
                            when {
                              formattedSpeed.length > 3 -> 16.sp
                              formattedSpeed.length > 2 -> 20.sp
                              else -> 26.sp
                            },
                        color = Color.Black,
                        textAlign = TextAlign.Center)

                    Text(
                        text = units.localizedString(context),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp,
                        color = Color.Gray)
                  }
            }
      }
}

@GreenScreenPreview
@Composable
fun ViennaConventionStyleSpeedLimitViewLowSpeedPreview() {
  ViennaConventionStyleSpeedLimitView(
      modifier = Modifier.padding(16.dp).shadow(4.dp, RoundedCornerShape(50)),
      speedLimit = MeasurementSpeed(30.0, MeasurementSpeedUnit.KilometersPerHour),
      units = MeasurementSpeedUnit.KilometersPerHour)
}

@GreenScreenPreview
@Composable
fun ViennaConventionStyleSpeedLimitViewModerateSpeedPreview() {
  ViennaConventionStyleSpeedLimitView(
      modifier = Modifier.padding(16.dp).shadow(4.dp, RoundedCornerShape(50)),
      speedLimit = MeasurementSpeed(300.0, MeasurementSpeedUnit.KilometersPerHour),
      units = MeasurementSpeedUnit.KilometersPerHour)
}

@GreenScreenPreview
@Composable
fun ViennaConventionStyleSpeedLimitViewHighSpeedPreview() {
  ViennaConventionStyleSpeedLimitView(
      modifier = Modifier.padding(16.dp).shadow(4.dp, RoundedCornerShape(50)),
      speedLimit = MeasurementSpeed(1000.0, MeasurementSpeedUnit.KilometersPerHour),
      units = MeasurementSpeedUnit.KilometersPerHour)
}
