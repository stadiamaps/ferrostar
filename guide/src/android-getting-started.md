# Getting Started on Android

This section of the guide covers how to integrate Ferrostar into an Android app.
We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

## Gradle setup

### Add dependencies

#### `build.gradle` with explicit version strings

If you’re using the classic `build.gradle`
with `implementation` strings using hard-coded versions,
here’s how to set things up.
Replace `X.Y.Z` with the latest [release version](https://github.com/orgs/stadiamaps/packages?repo_name=ferrostar).

```groovy
dependencies {
    // Elided: androidx dependencies: ktx and compose standard deps

    // Ferrostar
    def ferrostarVersion = 'X.Y.Z'
    implementation "com.stadiamaps.ferrostar:core:${ferrostarVersion}"
    implementation "com.stadiamaps.ferrostar:maplibreui:${ferrostarVersion}"

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

### Ensuring updates when the app loses focus

Note that Ferrostar does *not* require “background” location access!
This may be confusing if you’re new to mobile development.
On Android, we can use something called a *foreground service*
which lets us keep getting location updates even when the app isn’t front and center.
This is such a detailed topic that it gets its own page!
Learn about [Foreground Service](./android-foreground-service.md) configuration here.

### Location providers

You'll need to configure a provider to get location updates.
We bundle a few implementations to get you started, or you can create your own.
The broad steps are the same regardless of which provider you use:
create an instance of the class,
store it in an instance variable where it makes sense,
and (if simulating a route) set the location manually or enter a simulated route.

Similar to the Android location APIs you may already know,
you can add or remove listeners which will receive updates.

#### `AndroidSystemLocationProvider`

The `AndroidSystemLocationProvider` uses the location provider
from the Android open-source project.
This is not as good as the proprietary Google fused location client,
but it is the most compatible option
as it will run even on “un-Googled” phones
and can be used in apps distributed on F-Droid.

Initializing this provider requires an Android `Context`,
so you probably need to declare it as a `lateinit var` instance variable.

```kotlin
private lateinit var locationProvider: AndroidSystemLocationProvider
```

You can initialize it like so.
In an `Activity`, the context is simply `this`.
In other cases, get a context using an appropriate method.

```kotlin
locationProvider = AndroidSystemLocationProvider(context = this)
```

#### Google Play Fused Location Client

Alternatively, you can use the `FusedLocationProvider`
if your app uses Google Play Services.
This normally offers better device positioning than the default Android location provider
on supported devices.
To make use of it, 
you will need to include the optional `implementation "com.stadiamaps.ferrostar:google-play-services:${ferrostarVersion}"`
in your Gradle dependencies block.

Just as with the `AndroidSystemLocationProvider`,
you probably need to declare it as a `lateinit var` instance variable first,
and then initialize later once the `Context` is available.

```kotlin
// Instance variable definition
private lateinit var locationProvider: FusedLocationProvider

// Later when the activity loads and a context is available
locationProvider = FusedLocationProvider(context = this)
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
We use the popular OkHttp library for this.
Here we’ve set up a client with a global timeout of 15 seconds.
Refer to the [OkHttp documentation](https://square.github.io/okhttp/) for further details on configuration.

```kotlin
private val httpClient = OkHttpClient.Builder()
    .callTimeout(Duration.ofSeconds(15))
    .build()
```

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
          foregroundServiceManager = foregroundServiceManager
      )
```

`FerrostarCore` exposes a number of convenience constructors for common cases,
such as using a Valhalla [Route Provider](./route-providers.md#bundled-support).

`FerrostarCore` automatically subscribes to location updates from the `LocationProvider`.

## Set up voice guidance

Ferrostar is able to process spoken instructions generated from some routing engines.
The `com.stadiamaps.ferrostar.core.SpokenInstructionObserver` interface
specifies how to create your own observer.
A reference implementation is provided in the `AndroidTtsObserver` class,
which uses the text-to-speech engine built into Android.
PRs welcome for other popular services (ex: Amazon Polly;
note that some APIs also provide SSML instructions which work great with this!).

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
navigationViewModel =
    core.startNavigation(
        route = route,
        config =
        NavigationControllerConfig(
            StepAdvanceMode.RelativeLineStringDistance(
                minimumHorizontalAccuracy = 25U, automaticAdvanceDistance = 10U),
            RouteDeviationTracking.StaticThreshold(25U, 10.0)),
    )
```

Finally, If you’re simulating route progress
(ex: in the emulator) with a `SimulatedLocationProvider`),
set the route:

```kotlin
locationProvider.setSimulatedRoute(route)
```

## Using the `DynamicallyOrientingNavigationView`

We’re finally ready to turn that view model into a beautiful navigation map!
It’s really as simple as creating a `DynamicallyOrientingNavigationView` with the view model.
Here’s an example:

```kotlin
 val viewModel = navigationViewModel
 if (viewModel != null) {
     // You can get a free Stadia Maps API key at https://client.stadiamaps.com.
     // See https://stadiamaps.github.io/ferrostar/vendors.html for additional vendors
     DynamicallyOrientingNavigationView(
         styleUrl =
         "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=$stadiaApiKey",
         viewModel = viewModel) { uiState ->
         // You can add your own overlays here!
         // See https://github.com/Rallista/maplibre-compose-playground
     }
 } else {
     // Loading indicator
     Column(
         verticalArrangement = Arrangement.Center,
         horizontalAlignment = Alignment.CenterHorizontally) {
         Text(text = "Calculating route...")
         CircularProgressIndicator(modifier = Modifier.width(64.dp))
     }
 }
```

### Tools for Improving a NavigationView

- `KeepScreenOnDisposableEffect` is a simple disposable effect that automatically keeps the screen on and at consistent brightness while a user is on the scene using the effect. On dispose, the screen will return to default and auto lock and dim. See the demo app for an example of this being used alongside the `DynamicallyOrientingNavigationView`.

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/android/demo-app) with an example integration.

## Going deeper

This covers the basic “batteries included” configuration and pre-built UI.
But there’s a lot of room for customization!
Skip on over to the customization chapters that interest you.