package com.stadiamaps.ferrostar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.stadiamaps.ferrostar.DestinationSelection
import com.stadiamaps.ferrostar.R
import java.util.Locale
import uniffi.ferrostar.GeographicCoordinate

@Composable
fun DestinationSelectionBottomSheet(
    destination: DestinationSelection,
    onClose: () -> Unit,
    onStartNavigation: () -> Unit,
    onSheetHeightChanged: (Int) -> Unit,
) {
  Box(
      modifier = Modifier.fillMaxSize().systemBarsPadding(),
      contentAlignment = Alignment.BottomCenter,
  ) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .onSizeChanged { onSheetHeightChanged(it.height) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
      DestinationSelectionBottomSheetContent(
          destination = destination,
          onClose = onClose,
          onStartNavigation = onStartNavigation,
      )
    }
  }
}

@Composable
private fun DestinationSelectionBottomSheetContent(
    destination: DestinationSelection,
    onClose: () -> Unit,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier.padding(
              horizontal = 24.dp,
              vertical = 16.dp,
          )
  ) {
    Text(
        text =
            destination.label?.takeUnless { it.isBlank() }
              ?: stringResource(R.string.dropped_pin_title),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        text = stringResource(
            R.string.destination_coordinates,
            formatCoordinates(destination.coordinate),
        ),
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = onStartNavigation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
      Text(stringResource(R.string.start_navigation))
    }
    OutlinedButton(
        onClick = onClose,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp),
    ) {
      Text(stringResource(R.string.close_destination_sheet))
    }
  }
}

private fun formatCoordinates(coordinate: GeographicCoordinate): String =
    String.format(Locale.getDefault(), "%.5f, %.5f", coordinate.lat, coordinate.lng)

@Preview(showBackground = true)
@Composable
private fun DestinationSelectionBottomSheetContentPreview() {
  MaterialTheme {
    DestinationSelectionBottomSheetContent(
        destination =
            DestinationSelection(
                coordinate = GeographicCoordinate(
                    lat = 51.507778,
                    lng = -0.1275,
                ),
                label = "Trafalgar Square",
            ),
        onClose = {},
        onStartNavigation = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun DestinationSelectionBottomSheetContentWithoutLabelPreview() {
  MaterialTheme {
    DestinationSelectionBottomSheetContent(
        destination =
            DestinationSelection(
                coordinate = GeographicCoordinate(
                    lat = 34.5678,
                    lng = 45.6789,
                ),
            ),
        onClose = {},
        onStartNavigation = {},
    )
  }
}
