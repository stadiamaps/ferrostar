package expo.modules.ferrostar

import android.annotation.SuppressLint
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.intl.Locale
import com.stadiamaps.ferrostar.composeui.notification.DefaultForegroundNotificationBuilder
import com.stadiamaps.ferrostar.core.AndroidSystemLocationProvider
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.AndroidTtsStatusListener
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.core.extensions.fromOsrm
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import com.stadiamaps.ferrostar.googleplayservices.FusedLocationProvider
import expo.modules.ferrostar.extensions.toExpoRoute
import expo.modules.ferrostar.records.FerrostarCoreOptions
import expo.modules.ferrostar.records.NavigationOptions
import expo.modules.ferrostar.records.LocationMode
import expo.modules.ferrostar.records.NavigationControllerConfig
import uniffi.ferrostar.Route
import expo.modules.ferrostar.records.Route as ExpoRoute
import expo.modules.ferrostar.records.UserLocation
import expo.modules.ferrostar.records.Waypoint
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import uniffi.ferrostar.GeographicCoordinate
import java.time.Duration
import java.time.Instant

@SuppressLint("ViewConstructor")
class ExpoFerrostarView(context: Context, appContext: AppContext) :
    ExpoView(context, appContext), AndroidTtsStatusListener {

    private val httpClient by lazy {
        OkHttpClient
            .Builder()
            .callTimeout(Duration.ofSeconds(15))
            .build()
    }

    private lateinit var core: FerrostarCore
    private lateinit var viewModel: ExpoNavigationViewModel
    private lateinit var locationProvider: LocationProvider

    private val ttsObserver: AndroidTtsObserver by lazy { AndroidTtsObserver(context) }
    private val foregroundServiceManager: ForegroundServiceManager by lazy {
        FerrostarForegroundServiceManager(context, DefaultForegroundNotificationBuilder(context))
    }

    private val onNavigationStateChange by EventDispatcher()
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var coreOptions: FerrostarCoreOptions = FerrostarCoreOptions()
    private var navigationOptions: NavigationOptions = NavigationOptions()

    private lateinit var composeView: ComposeView

    private fun initializeResources() {
        Log.d("ExpoFerrostarNavigationView", "initializeResources")
        // Initialize ComposeView first
        composeView = ComposeView(context).also {
            it.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            addView(it)
        }

        // Initialize location provider
        locationProvider = when (coreOptions.locationMode) {
            LocationMode.FUSED -> FusedLocationProvider(context)
            LocationMode.SIMULATED -> SimulatedLocationProvider()
            else -> AndroidSystemLocationProvider(context)
        }

        if (locationProvider is SimulatedLocationProvider) {
            (locationProvider as SimulatedLocationProvider).lastLocation = uniffi.ferrostar.UserLocation(
                GeographicCoordinate(-43.525650, 172.639847),
                0.0,
                null,
                Instant.now(),
                null
            )
        }

        // Initialize core
        updateCore()

        // Setup TTS
        ttsObserver.statusObserver = this
    }

    init {
        Log.d("ExpoFerrostarNavigationView", "init")

        initializeResources()
        updateView()
    }

    private fun destroy() {
        Log.d("ExpoFerrostarNavigationView", "Destroying")
        core.stopNavigation()
        ttsObserver.shutdown()
    }

    fun setNavigationOptions(options: NavigationOptions) {
        navigationOptions = options
        if (::core.isInitialized) {
            updateView()
        } else {
            initializeResources()
        }
    }

    fun setCoreOptions(options: FerrostarCoreOptions) {
        coreOptions = options
        initializeResources() // Reinitialize with new options
    }

    private fun updateCore() {
        Log.d("ExpoFerrostarNavigationView", "Updating core")

        core = FerrostarCore(
            httpClient = httpClient,
            valhallaEndpointURL = this.coreOptions.valhallaEndpointURL,
            profile = this.coreOptions.profile,
            locationProvider = locationProvider,
            options = coreOptions.options,
            foregroundServiceManager = foregroundServiceManager,
            navigationControllerConfig = coreOptions.navigationControllerConfig.toConfig()
        )

        core.spokenInstructionObserver = ttsObserver
        updateViewModel()
    }

    fun startNavigation(route: ExpoRoute, options: NavigationControllerConfig?) {
        val config = options?.toConfig()

        try {
            core.startNavigation(route.toRoute(), config)
        } catch (e: Exception) {
            Log.e("ExpoFerrostarNavigationController", "Error starting navigation", e)
        }
    }

    fun stopNavigation(stopLocationUpdates: Boolean?) {
        if (stopLocationUpdates == null) {
            core.stopNavigation()
            return
        }

        core.stopNavigation(stopLocationUpdates)
    }

    fun createRouteFromOsrm(osrmRoute: String, waypoints: String): ExpoRoute {
        Log.d("ExpoFerrostarNavigationView", "Creating route from OSRM")
        Log.d("ExpoFerrostarNavigationView", "OSRM route: $osrmRoute")
        val routeByteArray = osrmRoute.toByteArray(Charsets.UTF_8)
        val waypointsByteArray = waypoints.toByteArray()
        val route = Route.fromOsrm(routeByteArray, waypointsByteArray, 6u)
        return Route.toExpoRoute(route)
    }

    fun replaceRoute(route: ExpoRoute, options: NavigationControllerConfig? = null) {
        core.replaceRoute(route.toRoute(), options?.toConfig())
    }

    fun advanceToNextStep() {
        core.advanceToNextStep()
    }

    suspend fun getRoutes(
        initialLocation: UserLocation,
        waypoints: List<Waypoint>
    ): List<ExpoRoute> {
        val location = initialLocation.toUserLocation()
        val points = waypoints.map { waypoint: Waypoint -> waypoint.toWaypoint() }
        var routes = emptyList<Route>()
        try {
            routes = core.getRoutes(location, points)
            Log.d("ExpoFerrostarNavigationController", "Got routes ${routes.size}")
        }
        catch (e: Exception) {
            Log.e("ExpoFerrostarNavigationController", "Error getting routes", e)
        }

        // Make this routes be casted to Route from uniffi.ferrostar.Route
        // and then to ExpoRoute
        val localRoutes = routes.map { currentRoute ->
            Route.toExpoRoute(currentRoute)
        }

        return localRoutes
    }

    fun setPreviewRoute(route: ExpoRoute) {
        if (!::viewModel.isInitialized) return
        viewModel.previewRoute.update { route.toRoute() }
    }

    private fun updateViewModel() {
        Log.d("ExpoFerrostarNavigationView", "Updating view model")
        viewModel = ExpoNavigationViewModel(core)

        mainScope.launch {
            viewModel.navigationUiState.collect { uiState ->
                onNavigationStateChange(
                    mapOf(
                        "isNavigating" to uiState.isNavigating(),
                        "isCalculatingNewRoute" to if (uiState.isCalculatingNewRoute != null) uiState.isCalculatingNewRoute!! else false,
                    )
                )
            }
        }
    }

    private fun updateView() {
        Log.d("ExpoFerrostarNavigationView", "Calculating view")

        composeView.removeAllViews()

        composeView.setContent {
            NavigationScene(viewModel = viewModel, locationProvider = locationProvider, options = navigationOptions)
        }
    }

    override fun onTtsInitialized(tts: TextToSpeech?, status: Int) {
        if (tts != null) {
            tts.setLanguage(Locale.current.platformLocale)
            Log.i("ExpoFerrostarNavigationView", "setLanguage status: $status")
        } else {
            Log.d("ExpoFerrostarNavigationView", "TTS setup failed! $status")
        }
    }

    override fun onTtsSpeakError(utteranceId: String, status: Int) {
        Log.e("ExpoFerrostarNavigationView", "Something went wrong synthesizing utterance $utteranceId. Status code: $status")
    }
}
