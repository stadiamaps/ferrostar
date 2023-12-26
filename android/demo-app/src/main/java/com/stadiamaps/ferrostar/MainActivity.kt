package com.stadiamaps.ferrostar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.SimulatedLocation
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.ui.theme.FerrostarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationController
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.StepAdvanceMode
import java.net.URL
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MainActivity : ComponentActivity() {
    private val initialSimulatedLocation = SimulatedLocation(
        GeographicCoordinate(-122.41970699999999, 37.807770999999995),
        6.0,
        null,
        Instant.now()
    )
    private val locationProvider = SimulatedLocationProvider()
    private val httpClient = OkHttpClient.Builder().build()

    // NOTE: This is a public instance which is suitable for development, but not for heavy use.
    // This server is suitable for testing and building your app, but once you are ready to go live,
    // YOU MUST USE ANOTHER SERVER.
    //
    // See https://github.com/stadiamaps/ferrostar/blob/main/VENDORS.md for options
    val core = FerrostarCore(
        valhallaEndpointURL = URL("https://valhalla1.openstreetmap.de/route"),
        profile = "bicycle",
        httpClient = httpClient
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var route by remember { mutableStateOf<Route?>(null) }
            var navigationController by remember { mutableStateOf<NavigationController?>(null) }

            LaunchedEffect(savedInstanceState) {
                // Fetch a route in the background
                launch(Dispatchers.IO) {
                    val routes = core.getRoutes(
                        initialSimulatedLocation.userLocation(), listOf(
                            GeographicCoordinate(-122.428411, 37.807587),
                        )
                    )

                    route = routes.first()
                    navigationController = NavigationController(
                        route!!,
                        NavigationControllerConfig(
                            StepAdvanceMode.RelativeLineStringDistance(
                                minimumHorizontalAccuracy = 25U,
                                automaticAdvanceDistance = 10U
                            )
                        )
                    )

                    // TODO: Non-toy route simulation
                    val delayBetweenSteps = 1.toDuration(DurationUnit.SECONDS)
                    for (coord in route!!.geometry) {
                        delay(delayBetweenSteps)
                        locationProvider.lastLocation = SimulatedLocation(
                            coord,
                            6.0,
                            null,
                            Instant.now()
                        )
                    }
                }
            }

            FerrostarTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val controller = navigationController
                    val r = route
                    if (controller != null && r != null) {
                        NavigationMapView(
                            // TODO: This constructor pattern is pretty whack. It's also probably the wrong way to create the ViewModel.
                            // Maybe the core should create the view model and/or controller?
                            viewModel = NavigationViewModel(
                                controller,
                                locationProvider,
                                initialSimulatedLocation,
                                r.geometry
                            )
                        )
                    } else {
                        // Loading indicator
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Calculating route...")
                            CircularProgressIndicator(modifier = Modifier.width(64.dp))
                        }
                    }
                }
            }
        }
    }
}
