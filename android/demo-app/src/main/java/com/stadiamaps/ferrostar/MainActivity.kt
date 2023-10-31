package com.stadiamaps.ferrostar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.ui.theme.FerrostarTheme
import okhttp3.OkHttpClient
import java.net.URL

class MainActivity : ComponentActivity() {
    val locationProvider = SimulatedLocationProvider()
    val httpClient = OkHttpClient.Builder().build()
    // TODO: Something useful. This is just a placeholder that essentially checks our ability to load the Rust library
    val core = FerrostarCore(
        valhallaEndpointURL = URL("https://api.stadiamaps.com/navigate/v1?api_key=YOUR-KEY-HERE"),
        profile = "pedestrian",
        locationProvider = locationProvider,
        httpClient = httpClient
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
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! 🦀 says 2 + 2 = 4",
        modifier = modifier,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FerrostarTheme {
        Greeting("Android")
    }
}