# Implementing Android Auto (Car App)

Ferrostar provides tooling to construct an Android Auto navigation app. The Demo App's
`auto` directory is a good reference implementation.

## Basic Setup

### Android Manifest

1. Add android auto permissions.
2. Add the navigation service to your app's manifest (e.g. `DemoCarAppService`).
3. Set the minimum car app version in the manifest `androidx.car.app.minCarApiLevel`. This can be configured based on which Car App Library features you use.
4. Add the automotive app descriptor and link it in your manifest `com.google.android.gms.car.application`.

The specific items shown in an abbreviated `AndroidManifest.xml`:

```xml
<manifest>

    <!-- Android Auto permissions -->
    <uses-permission android:name="androidx.car.app.MAP_TEMPLATES" />
    <uses-permission android:name="androidx.car.app.ACCESS_SURFACE" />
    <uses-permission android:name="androidx.car.app.NAVIGATION_TEMPLATES" />

    <application>
    
        <meta-data
            android:name="androidx.car.app.minCarApiLevel"
            android:value="2" />
            
        <meta-data
            android:name="com.google.android.gms.car.application"
            android:resource="@xml/automotive_app_desc" />

        <service
            android:name="com.stadiamaps.ferrostar.auto.DemoCarAppService"
            android:exported="true">
            <intent-filter>
                <action android:name="androidx.car.app.CarAppService" />
                <category android:name="androidx.car.app.category.NAVIGATION" />
            </intent-filter>
        </service>

    </application>
</manifest>
```

And the file `automotive_app_desc.xml`: 

```xml
<?xml version="1.0" encoding="utf-8"?>
<automotiveApp>
    <uses name="template" />
</automotiveApp>
```

### Car App Service

Extend `CarAppService` and return a `Session` from `onCreateSession`. In your `Session`,
initialize any app-level dependencies and return your navigation `Screen`:

```kotlin
class MyCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }

    override fun onCreateSession(sessionInfo: SessionInfo): Session = MyCarAppSession()
}

class MyCarAppSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        AppModule.init(carContext)
        val destination = NavigationIntentParser().parse(intent)
        return MyNavigationScreen(carContext, initialDestination = destination)
    }
}
```

### Car App Screen

Extend `ComposableScreen` from the MapLibre Compose Car App library. Your screen is
responsible for three things: managing the `SurfaceAreaTracker`, building the
`NavigationTemplate`, and rendering the map surface.

#### Surface Area Tracking

Create a `SurfaceAreaTracker` and register it as the surface gesture callback in one step. 

```kotlin
private val surfaceAreaTracker = SurfaceAreaTracker { surfaceGestureCallback = it }
```

This bridges surface area events (stable area, visible area, gestures) into
Compose-observable state. Pass it to `CarAppNavigationView`, which handles safe-area
overlay placement and gesture wiring internally.

#### Navigation Manager Bridge

Wire up `NavigationManagerBridge` to connect Ferrostar's view model to the Car App Library's `NavigationManager`. 
This handles the navigation lifecycle (NF-5), trip updates (NF-4), and turn-by-turn notifications (NF-3):

```kotlin
private val navigationManagerBridge = NavigationManagerBridge(
    navigationManager = carContext.getCarService(NavigationManager::class.java),
    viewModel = viewModel,
    context = carContext,
    notificationManager = TurnByTurnNotificationManager(carContext, R.drawable.ic_navigation),
    onStopNavigation = { viewModel.stopNavigation() },
    onAutoDriveEnabled = { viewModel.enableAutoDriveSimulation() },
    isCarForeground = { lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
)
```

Call `navigationManagerBridge.start(scope)` in your `init` block and
`navigationManagerBridge.stop()` in your `onDestroy` lifecycle observer.

#### Composable Content

In `content()`, use `screenSurfaceState` to observe the stable area for camera padding,
and pass the tracker to `CarAppNavigationView`:

```kotlin
@Composable
override fun content() {
    val surfaceArea by screenSurfaceState(surfaceAreaTracker)
    val camera = remember { mutableStateOf(viewModel.mapViewCamera.value) }

    CarAppNavigationView(
        modifier = Modifier.fillMaxSize(),
        styleUrl = myMapStyleUrl,
        camera = camera,
        viewModel = viewModel,
        surfaceAreaTracker = surfaceAreaTracker,
    )
}
```

#### Navigation Template

In `onGetTemplate()`, use `NavigationTemplateBuilder` to produce the appropriate template for the current navigation state:

```kotlin
override fun onGetTemplate(): Template {
    uiState?.let { state ->
        if (state.isNavigating()) {
            return NavigationTemplateBuilder(carContext)
                .setTripState(state.tripState)
                .setOnStopNavigation { viewModel.stopNavigation() }
                .build()
        }
    }
    return buildMyMapTemplate()
}
```

## Requirements

Google has specific review guidelines for Android Auto navigation apps. You can
find them here: [Car App Quality Guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality).
Search for `NF` to find the navigation app specific guidelines.

This document summarizes the navigation app guidelines (as of March 2026) and
provides guidance on how Ferrostar can be used to implement them in your
Android Auto app.

### NF-1 - Turn by Turn Navigation

> The app must provide turn-by-turn navigation directions.

`NavigationTemplateBuilder` translates Ferrostar's active navigation state into a `NavigationTemplate` for Android Auto,
including maneuver icons, distance/time estimates, lane guidance, and action strip controls.

### NF-2 - Only Map Content on the Surface (with Exceptions)

> The app draws only map content on the surface of the navigation templates. Text-based
> turn-by-turn directions, lane guidance, and estimated arrival time must be displayed on
> the relevant components of the navigation template. Additional information relevant to the
> drive, speed limit, road obstructions, etc., can be drawn on the safe area of the map.

`CarAppNavigationView` handles this automatically. Pass a `SurfaceAreaTracker` and the view constrains speed
limit and road name overlays to the display's composite stable area, keeping them clear
of the template chrome.

### NF-3 - Turn by Turn Notifications

> When the app provides text-based turn-by-turn directions, it must also trigger navigation
> notifications. For more information, see
> [Turn-by-turn notifications](https://developer.android.com/training/cars/apps/navigation#turn-by-turn-notifications).

`TurnByTurnNotificationManager` posts heads-up notifications for each new spoken instruction. Pass it to
`NavigationManagerBridge`, which suppresses notifications automatically when the car
screen is in the foreground (the map surface is already showing guidance).

### NF-4 - Next Step to Cluster

> When the navigation app provides text-based turn-by-turn directions, it must send
> next-turn information to the vehicle's cluster display. For more information, see
> [Navigation metadata](https://developer.android.com/training/cars/apps/navigation#navigation-metadata).

`NavigationManagerBridge` calls `NavigationManager.updateTrip()` on every navigation state change. The trip is
built by `FerrostarTrip`, which populates the current step (maneuver and instruction),
travel estimate (distance and ETA), current road name, and destination — the fields
the vehicle cluster display reads.

### NF-5 - Don't Interfere if not Navigating

> The app must not provide turn-by-turn notifications, voice guidance, or cluster
> information when another navigation app is providing turn-by-turn instructions. For more
> information, see
> [Start, end, and stop navigation](https://developer.android.com/training/cars/apps/navigation#starting-ending-stopping-navigation).

`NavigationManagerBridge` calls `NavigationManager.navigationStarted()` and `NavigationManager.navigationEnded()`
at the correct lifecycle points, letting the Car App host arbitrate between competing
navigation apps.

### NF-6 - App Must Handle Navigation Intents

> The app must handle navigation requests from other apps. For more information, see
> [Support navigation intents](https://developer.android.com/training/cars/apps/navigation#support-navigation-intents).

`NavigationIntentParser` parses incoming navigation intents into a `NavigationDestination`.
It supports `geo:` and `google.navigation:` URI schemes out of the box and is `open`
for subclassing to support additional schemes:

```kotlin
class MyIntentParser : NavigationIntentParser() {
    override fun parseUri(uri: Uri) = parseMyScheme(uri) ?: super.parseUri(uri)
}
```

Call it from `Session.onCreateScreen` and pass the result to your navigation screen:

```kotlin
override fun onCreateScreen(intent: Intent): Screen {
    val destination = NavigationIntentParser().parse(intent)
    return MyNavigationScreen(carContext, initialDestination = destination)
}
```

### NF-7 - Test Drive Mode

> The app must provide a "test drive" mode that simulates driving. For more information,
> see [Simulate navigation](https://developer.android.com/training/cars/apps/navigation#simulating-navigation).

Pass an `onAutoDriveEnabled` callback to `NavigationManagerBridge`. It is invoked when
the Car App host requests simulation (e.g. during review):

```kotlin
NavigationManagerBridge(
    ...
    onAutoDriveEnabled = { viewModel.enableAutoDriveSimulation() },
)
```

You can also trigger auto-drive manually via adb for testing:

```sh
adb shell dumpsys activity service com.stadiamaps.ferrostar.auto.DemoCarAppService AUTO_DRIVE
```
