# Getting Started on Android

TODO.

## Add the Maven dependencies

## Configure location services

### Permissions and privacy settings

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

Optionally, once you have a route, simulate the replay of the route.
You can set a `warpFactor` to play it back faster.

```kotlin
locationProvider.warpFactor = 2u
locationProvider.setSimulatedRoute(route)
```

#### TODO: Google Play Services-backed provider

## Configure the `FerrostarCore` instance

`FerrostarCore` automatically subscribes to location updates from the `LocationProvider`.

## OPTIONAL: Configure the `RouteDeviationHandler`

## Using the NavigationMapView

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/android/demo-app) with an example integration.
