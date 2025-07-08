# Getting Started on Android

This section of the guide covers how to integrate Ferrostar into an Android app.
We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

## Gradle setup

### Add dependencies

Let’s get started with Gradle setup.
Replace `X.Y.Z` with the latest [release version](https://central.sonatype.com/namespace/com.stadiamaps.ferrostar).

#### `build.gradle` with explicit version strings

If you’re using the classic `build.gradle`
with `implementation` strings using hard-coded versions,
here’s how to set things up.

```groovy
dependencies {
    // Elided: androidx dependencies: ktx and compose standard deps

    // Ferrostar
    def ferrostarVersion = 'X.Y.Z'
    implementation "com.stadiamaps.ferrostar:core:${ferrostarVersion}"
    implementation "com.stadiamaps.ferrostar:maplibreui:${ferrostarVersion}"
    implementation "com.stadiamaps.ferrostar:composeui:${ferrostarVersion}"

    // Optional - if using Google Play Service's FusedLocation
    implementation "com.stadiamaps.ferrostar:google-play-services:${ferrostarVersion}"

    // okhttp3
    implementation platform("com.squareup.okhttp3:okhttp-bom:4.11.0")
    implementation 'com.squareup.okhttp3:okhttp'
}
```

#### `libs.versions.toml` “modern style”

If you’re using the newer `libs.versions.toml` approach,
add the versions like so:

```toml
[versions]
ferrostar = "X.Y.X"
okhttp3 = "4.11.0"

[libraries]
ferrostar-core = { group = "com.stadiamaps.ferrostar", name = "core", version.ref = "ferrostar" }
ferrostar-maplibreui = { group = "com.stadiamaps.ferrostar", name = "maplibreui", version.ref = "ferrostar" }
okhttp3 = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp3" }
```

Then reference it in your `build.gradle`:

```groovy
dependencies {
    // Elided: androidx dependencies: ktx and compose standard deps

    // Ferrostar
    implementation libs.ferrostar.core
    implementation libs.ferrostar.maplibreui

    // okhttp3
    implementation libs.okhttp3
}
```

## Configure location services

### Declaring permissions used

Your app will need access to the user’s location.
First, you’ll need the requisite permissions in your Android manifest:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

You’ll then need to request permission from the user to access their precise location.

### Requesting location access

The “best” way to do this tends to change over time and varies with your app structure.
If you’re using Jetpack Compose,
you’ll want the `rememberLauncherForActivityResult` API.
If you’re just using plain activities,
the `registerForActivityResult` has what you need.

In either case, you’ll want to review [Google’s documentation](https://developer.android.com/develop/sensors-and-location/location/permissions#kotlin).

<div class="warning">

Note that Ferrostar does *not* require “background” location access!
This may be confusing if you’re new to mobile development.
On Android, we can use something called a *foreground service*
which lets us keep getting location updates even when the app isn’t front and center.
This is such a detailed topic that it gets its own page!
Learn about [Foreground Service](./android-foreground-service.md) configuration here.

</div>

### Location providers

You'll need to configure a provider to get location updates.
We bundle a few implementations to get you started, or you can create your own.
The broad steps are the same regardless of which provider you use:
create an instance of the class,
store it in an instance variable where it makes sense,
and (if simulating a route) set the location manually or enter a simulated route.

Similar to the Android location APIs you may already know,
you can add or remove listeners which will receive updates.


#### Google Play Fused Location Client

If your app uses Google Play Services,
you can use the `FusedLocationProvider`
This normally offers better device positioning than the default Android location provider
on supported devices.
To make use of it,
you will need to include the optional `implementation "com.stadiamaps.ferrostar:google-play-services:${ferrostarVersion}"`
in your Gradle dependencies block.

You can initialize the provider like so.
In an `Activity`, the context is simply `this`.
In other cases, get a context using an appropriate method.

```kotlin
locationProvider = FusedLocationProvider(context = this)
```

#### `AndroidSystemLocationProvider`

The `AndroidSystemLocationProvider` uses the location provider
from the Android open-source project.

<div class="warning">

This is not as good as the proprietary Google fused location client.
You can generally expect significantly worse raw positioning,
but you may need this in a couple scenarios:

1. You need to run on *any* Android phone, including ones without Google Play Services.
2. You want to distribute your app on F-Droid, which requires all apps use open-source software.
3. You’re doing something really low level and need the raw positioning info from specific sensors.

Otherwise, you’re probably better off using the Fused Location Client from Google.

</div>

You can initialize the provider like so.
In an `Activity`, the context is simply `this`.
In other cases, get a context using an appropriate method.

```kotlin
locationProvider = AndroidSystemLocationProvider(context = this)
```

#### `SimulatedLocationProvider`

The `SimulatedLocationProvider` allows for simulating location within Ferrostar
without needing GPX files or complicated environment setup.
This is great for testing and development without stepping outside.

First, instantiate the class.
This will typically be saved as an instance variable.

```kotlin
private val locationProvider = SimulatedLocationProvider()
```

Later, most likely somewhere in your activity creation code or similar,
set a location to your desired simulation start point.

```kotlin
locationProvider.lastLocation = initialSimulatedLocation
```

Once you have a route, you can simulate the replay of the route.
This is technically optional (you can just set `lastLocation` yourself too),
but playing back a route saves you the effort.
You can set a `warpFactor` to play it back faster.

```kotlin
locationProvider.warpFactor = 2u
locationProvider.setSimulatedRoute(route)
```

You don’t need to do anything else after setting a simulated route;
`FerrostarCore` will automatically add itself as a listener,
which will trigger updates.

## Configure an HTTP client

Before we configure the Ferrostar core, we need to set up an HTTP client.
This is typically stored as an instance variable in one of your classes (ex: activity).
We use the popular OkHttp library for this, but the Core is configured to allow alternatives through the HttpClientProvider interface.
Here we’ve set up a client with a global timeout of 15 seconds.
Refer to the [OkHttp documentation](https://square.github.io/okhttp/) for further details on configuration.

```kotlin
private val httpClient = OkHttpClient.Builder()
    .callTimeout(Duration.ofSeconds(15))
    .build()
    .toOkHttpClientProvider()
```

### (Optional) Configure annotation parsing

Want to get speed limit information?
Or have your own live traffic layers?
Annotations provide a way to bring this information into your routes.
We support some standard ones out of the box.
Check out the chapter on [annotations](annotations.md) for details.

## Configure the `FerrostarCore` instance

We now have all the pieces we need to set up a `FerrostarCore` instance!
The `FerrostarCore` instance should live for at least the duration of a navigation session.
Bringing it all together, a typical init looks something like this:

```kotlin
private val core =
      FerrostarCore(
          valhallaEndpointURL = URL("https://api.stadiamaps.com/route/v1?api_key=YOUR-API-KEY"),
          profile = "bicycle",
          httpClient = httpClient,
          locationProvider = locationProvider,
          foregroundServiceManager = foregroundServiceManager,
          navigationControllerConfig =
            NavigationControllerConfig(
                WaypointAdvanceMode.WaypointWithinRange(100.0),
                stepAdvanceDistanceEntryAndExit(30u, 5u, 32u),
                // This is a special condition used for the last two steps of the route. As we can't assume the
                // user continue moving past the step like the other conditions.
                stepAdvanceDistanceToEndOfStep(30u, 32u),
                RouteDeviationTracking.StaticThreshold(15U, 50.0),
                CourseFiltering.SNAP_TO_ROUTE),
      )
```

`FerrostarCore` exposes a number of convenience constructors for common cases,
such as using a Valhalla [Route Provider](./route-providers.md#bundled-support),
and automatically subscribes to location updates from the `LocationProvider`.

There are a LOT of options, but don’t worry; everything is documented.
Check out the [Navigation Behavior](configuring-the-navigation-controller.md)
customization chapter for details.

## Set up voice guidance

If your routes include spoken instructions,
Ferrostar can trigger the speech synthesis at the right time.
Ferrostar includes the `AndroidTtsObserver` class,
which uses the text-to-speech engine built into Android.

The `AndroidTtsObserver` follows lifecycle recommendations from the Android documentation,
[TextToSpeech shutdown behavior](https://developer.android.com/reference/android/speech/tts/TextToSpeech#shutdown()).
This design means your activity should call `shutdown` on the observer in the `onDestroy` method and start it again
in `onStart` if you want to continue using it. If the instance is shut down and not started again, you will not have spoken instructions.

```kotlin
override fun onStart() {
    super.onStart()
    ttsObserver.start()
}

override fun onDestroy() {
    super.onDestroy()
    ttsObserver.shutdown()
}
```

You can also use your own implementation,
such as a local AI model or cloud service like Amazon Polly.
The `com.stadiamaps.ferrostar.core.SpokenInstructionObserver` interface
specifies the required API.
PRs welcome to add other publicly accessible speech API implementations.

**TODO documentation:**

* Android Manifest
* Set the language
* Additional config (you have full control; link to Android docs)

## Getting a route

Getting a route is easy!
All you need is the start location (from the location provider)
and a list of at least one waypoint to visit.

```kotlin
val routes =
    core.getRoutes(
        userLocation,
        listOf(
            Waypoint(
                coordinate = GeographicCoordinate(37.807587, -122.428411),
                kind = WaypointKind.BREAK),
        ))
```

Note that this is a `suspend` function, so you’ll need to use it within a coroutine scope.
You probably want something like `launch(Dispatchers.IO) { .. }`
for most cases to ensure it’s running on the correct dispatcher.
You may select a different dispatcher if you are doing offline route calculation.

## Starting a navigation session

Once you have a route (ex: by grabbing the first one from the list
or by presenting the user with a selection screen),
you’re ready to start a navigation session!

When you start a navigation session, the core returns a view model
which will automatically be updated with state updates.

You can save “rememberable” state somewhere near the top of your composable block like so:

```kotlin
var navigationViewModel by remember { mutableStateOf<NavigationViewModel?>(null) }
```

And then use it to store the result of your `startNavigation` invocation:

```kotlin
core.startNavigation(
    route = route
    // NOTE: You can also change your config here with an optional config parameter!
)
```

Finally, If you’re simulating route progress
(ex: in the emulator) with a `SimulatedLocationProvider`),
set the route:

```kotlin
locationProvider.setSimulatedRoute(route)
```

## Using the `DynamicallyOrientingNavigationView`

We’re finally ready to put this together into a beautiful navigation map!
`FerrostarCore` exposes a state flow,
which you can incorporate into your own view model,
which must implement the `NavigationViewModel` interface.
See the `DemoNavigationViewModel` for an example of what a view model might look like.

Here’s an example:

```kotlin
 // You can get a free Stadia Maps API key at https://client.stadiamaps.com.
 // See https://stadiamaps.github.io/ferrostar/vendors.html for additional vendors
 DynamicallyOrientingNavigationView(
     styleUrl =
     "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=$stadiaApiKey",
     viewModel = viewModel) { uiState ->
     // You can add your own overlays here!
     // See the DemoNavigationScene or https://github.com/Rallista/maplibre-compose-playground
     // for some examples.
 }
```

### Tools for Improving a NavigationView

- `KeepScreenOnDisposableEffect` is a simple disposable effect that automatically keeps the screen on and at consistent brightness while a user is on the scene using the effect. On dispose, the screen will return to default and auto lock and dim. See the demo app for an example of this being used alongside the `DynamicallyOrientingNavigationView`.

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/android/demo-app)
to show how to integrate Ferrostar into your Android app.

## Going deeper

This covers the basic “batteries included” configuration and pre-built UI.
But there’s a lot of room for customization!
Skip on over to the customization chapters that interest you.
