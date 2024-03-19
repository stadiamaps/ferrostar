# Getting Started on Android

TODO.

## Add the Maven dependencies

## Configure location services

### Permissions and privacy settings

### Location providers

Location providers do pretty much what you would expect: provide locations!
Location can come from a variety of underlying sources.
If you're somewhat experienced building Android apps,
you'll probably immediately think of either the Google `FusedLocationProviderClient`
or the `LocationManager` class in AOSP.
Additionally, location can also come from a simulation
or a third-party location SDK such as [Naurt](https://naurt.com/).

To support the variety of use cases, Ferrostar introduces the `LocationProvider` interface as a common abstraction.
You'll need to provide a concrete object implementing `LocationProvider` to use Ferrostar.

We bundle a few implementations to get you started, or you can create your own.

#### TODO: Google Play Services-backed provider

#### `SimulatedLocationProvider`

The `SimulatedLocationProvider` allows for simulating location within Ferrostar,
without needing GPX files or complicated environment setup.

To simulate an entire route from start to finish,
use the higher level `setSimulatedRoute` function to preload an entire route,
which will be "played back" automatically when there is a listener attached.
You can control the simulation speed by setting the `warpFactor` property.

If you want low-level control instead, you can just set properties like `lastLocation` and `lastHeading` directly.

## Configure the `FerrostarCore` instance

## OPTIONAL: Configure the `RouteDeviationHandler`

## Using the NavigationMapView

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/android/demo-app) with an example integration.
