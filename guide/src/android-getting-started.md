# Getting Started on Android

This section of the guide covers how to integrate Ferrostar into an Android app.
We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

## Add dependencies

Ferrostar releases are hosted on GitHub Packages.
You’ll need to authenticate first in order to access them.
GitHub has a [guide on setting this up](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).

### Maven repository setup

Once you’ve configured GitHub credentials as project properties or environment variables,
Add the repostory to your build script.
Here is example `build.gradle`:

```groovy
repositories {
    google()
    mavenCentral()

    maven {
        url = uri("https://maven.pkg.github.com/stadiamaps/ferrostar")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.token") ?: System.getenv("TOKEN")
        }
    }
}
```

### Add dependencies

Next, add the dependencies to your app’s `build.gradle`.
We omit the standard Jetpack Compose dependencies here.

```groovy
dependencies {
    // Elided: androidx dependencies: ktx and compose standard deps

    // Ferrostar
    def ferrostarVersion = 'X.Y.Z' // Replace with the latest version
    implementation "com.stadiamaps:ferrostar-core:${ferrostarVersion}"
    implementation "com.stadiamaps:ferrostar-maplibreui:${ferrostarVersion}"

    implementation platform("com.squareup.okhttp3:okhttp-bom:4.10.0")
    implementation 'com.squareup.okhttp3:okhttp'
}
```

## Configure location services

### TODO: Permissions and privacy settings

The usual Android setup.

### Location providers

You'll need to configure a provider to get location updates.
We bundle a few implementations to get you started, or you can create your own.
The broad steps are the same regardless of which provider you use:
create an instance of the class,
store it in an instance variable where it makes sense,
and (if simulating a route) set the location manually or enter a simulated route.

Similar to the Android location APIs you may already know,
you can add or remove listeners which will receive updates.

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

#### TODO: Google Play Services-backed provider

## Configure an HTTP client

Before we configure the Ferrostar core, we need to set up an HTTP client.
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
      )
```

`FerrostarCore` exposes a number of convenience constructors for common cases,
such as using a Valhalla [Route Provider](./route-providers.md#bundled-support).

`FerrostarCore` automatically subscribes to location updates from the `LocationProvider`.

## OPTIONAL: Configure a `RouteDeviationHandler`

By default, Ferrostar will fetch new routes when the user goes off course.
You can override this behavior by setting the `deviationHandler` property.
This property is a `fun interface` in Kotlin, which allows for concise in-line declarations
if your project uses Kotlin.

Refer to the demo app for an example of a custom implementation.
Note that you can disable the default behavior of attempting to reroute
by setting an empty implementation (not setting the property to `null`!).

## OPTIONAL: Configure an `AlternativeRouteProcessor`

`FerrostarCore` may occasionally load alternative routes.
At this point, this is only used for recalculating when the user deviates from the route,
but it may be used for additional behaviors in the future,
such as speculative route checking based on current traffic (for supported vendors).
By default, this will “accept” the new route and reroute the active navigation session
whenever a new route arrives due to the user going off course.

If you don’t set the `alternativeRouteProcessor`
Refer to the demo app for an example of a custom implementation.
Note that you can disable the default behavior of attempting to reroute
by setting an empty implementation (not setting the property to `null`!).

## TODO: Using the NavigationMapView

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/android/demo-app) with an example integration.
