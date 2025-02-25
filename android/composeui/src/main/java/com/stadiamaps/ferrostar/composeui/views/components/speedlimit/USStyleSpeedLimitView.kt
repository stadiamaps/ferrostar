package com.stadiamaps.ferrostar.composeui.views.components.speedlimit

import android.content.Context
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stadiamaps.ferrostar.composeui.R
import com.stadiamaps.ferrostar.composeui.formatting.MeasurementSpeedFormatter
import com.stadiamaps.ferrostar.composeui.measurement.localizedString
import com.stadiamaps.ferrostar.composeui.support.GreenScreenPreview
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit

@Composable
fun USStyleSpeedLimitView(
    modifier: Modifier = Modifier,
    speedLimit: MeasurementSpeed,
    units: MeasurementSpeedUnit = MeasurementSpeedUnit.MilesPerHour,
    context: Context = LocalContext.current,
    formatter: MeasurementSpeedFormatter = MeasurementSpeedFormatter(context, speedLimit),
    locale: ULocale = ULocale.getDefault()
) {
  val formattedSpeed = formatter.formattedValue(locale, units)

  Box(
      modifier =
          modifier
              .height(84.dp)
              .width(60.dp)
              .background(color = Color.White, shape = RoundedCornerShape(8.dp))
              .padding(2.dp)) {
        Box(
            modifier =
                Modifier.height(80.dp)
                    .width(56.dp)
                    .background(color = Color.Black, shape = RoundedCornerShape(6.dp))
                    .padding(2.dp)) {
              Box(
                  modifier =
                      Modifier.height(76.dp)
                          .width(52.dp)
                          .background(color = Color.White, shape = RoundedCornerShape(4.dp))
                          .padding(4.dp)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top) {
                          Text(
                              text = stringResource(R.string.speed).uppercase(),
                              fontSize = 9.sp,
                              lineHeight = 10.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color.Black)

                          Text(
                              text = stringResource(R.string.limit).uppercase(),
                              fontSize = 9.sp,
                              lineHeight = 10.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color.Black)

                          Spacer(modifier = Modifier.weight(1f))

                          Text(
                              text = formattedSpeed,
                              fontSize =
                                  when {
                                    (formattedSpeed.length < 3) -> 30.sp
                                    (formattedSpeed.length == 3) -> 24.sp
                                    else -> 18.sp
                                  },
                              lineHeight = if (formattedSpeed.length > 3) 26.sp else 32.sp,
                              fontWeight = FontWeight.ExtraBold,
                              color = Color.Black,
                              textAlign = TextAlign.Center)

                          Text(
                              text = units.localizedString(context),
                              fontSize = 9.sp,
                              lineHeight = 10.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color.Gray)
                        }
                  }
            }
      }
}

@GreenScreenPreview
@Composable
fun USStyleSpeedLimitViewLowSpeedPreview() {
  USStyleSpeedLimitView(
      modifier = Modifier.padding(16.dp).shadow(4.dp),
      speedLimit = MeasurementSpeed(55.0, MeasurementSpeedUnit.MilesPerHour))
}

@GreenScreenPreview
@Composable
fun USStyleSpeedLimitViewModerateSpeedPreview() {
  USStyleSpeedLimitView(
      modifier = Modifier.padding(16.dp).shadow(4.dp),
      speedLimit = MeasurementSpeed(100.0, MeasurementSpeedUnit.MilesPerHour))
}

@GreenScreenPreview
@Composable
fun USStyleSpeedLimitViewHighSpeedPreview() {
  USStyleSpeedLimitView(
      modifier = Modifier.padding(16.dp).shadow(4.dp),
      speedLimit = MeasurementSpeed(1000.0, MeasurementSpeedUnit.MilesPerHour))
}
