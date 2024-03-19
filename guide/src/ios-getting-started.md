# Getting Started on iOS

This section of the guide covers how to integrate Ferrostar into an iOS app.
We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

TODO

## Add the Swift package dependency

## Configure location services

### Permissions and privacy strings

### Location providers

Location providers do pretty much what you would expect: provide locations!
Location can come from a variety of underlying sources.
If you're somewhat experienced building iOS apps, you'll probably immediately think of `CLLocationManager`,
but location can also come from a simulation or a third-party location SDK such as [Naurt](https://naurt.com/).

To support the variety of use cases, Ferrostar introduces the `LocationProviding` protocol
as a common interface.
You'll need to provide an instance of some concrete type conforming to `LocationProviding` to use Ferrostar.

We bundle a few implementations to get you started, or you can create your own.

#### `CoreLocationProvider`

The `CoreLocationProvider` provides a ready-to-go wrapper around a `CLLocationManager`.
It automatically takes care of requesting sensible permissions for you
(make sure you have permission strings set in your `Info.plist`!).
This is what you want for most production use cases.

#### `SimulatedLocationProvider`

The `SimulatedLocationProvider` allows for simulating location within Ferrostar,
without needing GPX files or complicated environment setup.

To simulate an entire route from start to finish,
use the higher level `setSimulatedRoute:` function to preload an entire route,
which will be "played back" whenever location updates are enabled.
You can control the simulation speed by setting the `warpFactor` property.

If you want low-level control instead, you can just set properties like `lastLocation` and `lastHeading` directly.

## Configure the `FerrostarCore` instance

## OPTIONAL: Configure the core delegate

## Using the NavigationMapView

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/apple/DemoApp) with an example integration.
