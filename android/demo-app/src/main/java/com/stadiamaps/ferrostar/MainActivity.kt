package com.stadiamaps.ferrostar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.SimulatedLocation
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.ui.theme.FerrostarTheme
import okhttp3.OkHttpClient
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.NavigationController
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteStep
import uniffi.ferrostar.StepAdvanceMode
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent
import java.net.URL
import java.time.Instant

class MainActivity : ComponentActivity() {
    // TODO: Move to some sort of test fixtures
    private val geom = listOf(
        GeographicCoordinate(-122.41970699999999, 37.807770999999995),
        GeographicCoordinate(-122.42041599999999, 37.807680999999995),
        GeographicCoordinate(-122.42040399999999, 37.807623),
        GeographicCoordinate(-122.420678, 37.807587),
    )
    // Maybe the core should create the view model and expose it via a property...
    private val simulatedLocation =
        SimulatedLocation(geom.first(), 6.0, null, Instant.now())
    private val locationProvider = SimulatedLocationProvider()
    private val httpClient = OkHttpClient.Builder().build()

    // TODO: Something useful. This is just a placeholder that essentially checks our ability to load the Rust library
    val core = FerrostarCore(
        valhallaEndpointURL = URL("https://api.stadiamaps.com/route/v1?api_key=YOUR-KEY-HERE"),
        profile = "pedestrian",
        httpClient = httpClient
    )
    private val route = Route(
        geometry = geom,
        distance = 100.0,
        steps = listOf(
            RouteStep(
                geom, 100.0, "Jefferson St.", "Walk west on Jefferson St.", listOf(
                    VisualInstruction(
                        VisualInstructionContent(
                            "Hyde Street",
                            ManeuverType.TURN,
                            ManeuverModifier.LEFT,
                            null
                        ),
                        null,
                        42.0
                    )
                ),
                listOf()
            )
        ),
        waypoints = listOf(geom.first(), geom.last())
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FerrostarTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationMapView(
                        // TODO: This constructor pattern is pretty whack. It's also probably the wrong way to create the ViewModel.
                        viewModel = NavigationViewModel(
                            NavigationController(
                                route = route,
                                config = NavigationControllerConfig(
                                    StepAdvanceMode.RelativeLineStringDistance(
                                        40U,
                                        15U
                                    )
                                )
                            ),
                            locationProvider,
                            simulatedLocation,
                            geom
                        )
                    )
                }
            }
        }
    }
}
