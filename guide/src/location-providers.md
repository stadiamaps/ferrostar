# Location Providers

Location providers do what you would expect: provide locations!
Location providers are included in the platform libraries,
since they need to talk to the outside world.

Location can come from a variety of sources.
If you're somewhat experienced building mobile apps,
you may think of `CLLocationManager` on iOS, or the `FusedLocationProviderClient` on Android.
In addition to the usual platform location services APIs,
location can also come from a simulation or a third-party location SDK such as [Naurt](https://naurt.com/).

To support this variety of use cases,
Ferrostar introduces the `LocationProvider` protocol (iOS) / interface (Android) as a common abstraction.
We bundle a few implementations to get you started, or you can create your own.

## `SimulatedLocationProvider`

The `SimulatedLocationProvider` allows for simulating location within Ferrostar,
without needing GPX files or complicated environment setup.
The usage patterns are roughly the same on iOS and Android:

1. Create an instance of the `SimulatedLocationProvider` class.
2. Set a location _or_ a `Route`.
3. Let the `FerrostarCore` handle the rest.

To simulate an entire route from start to finish,
use the higher level `setSimulatedRoute` function to preload an entire route,
which will be "played back" automatically when there is a listener attached.
You can control the simulation speed by setting the `warpFactor` property.

NOTE: While the `SimulatedLocationProvider` is defined in the platform library layer,
the simulation functionality comes from the functional core and is implemented in Rust
for better testability and guaranteed consistency across platforms.

If you want low-level control instead, you can just set properties like `lastLocation` and `lastHeading` directly.

You can grab a location manually (ex: to fetch a route; both iOS and Android provide observation capabilities).
`FerrostarCore` also automatically subscribes to location updates during navigation,
and unsubscribes itself (Android) or stops location updates automatically (iOS) to conserve battery power.

## "Live" Location Providers

Ferrostar includes the following live location providers:

* iOS
  - `CoreLocationProvider` - Location backed by a `CLLocationManager`. See the [iOS tutorial](./ios-getting-started.md#corelocationprovider) for a usage example.
* Android
  - [`AndroidSystemLocationProvider`] - Location backed by an `android.location.LocationManger` (the class that is included in AOSP). See the [Android tutorial](./android-getting-started.md#androidsystemlocationprovider) for a usage example.
  - [`FusedLocationProvider`] - Location backed by a Google Play Services `FusedLocationClient`, which is proprietary but often provides better location updates. See the [Android tutorial](./android-getting-started.md#google-play-fused-location-client) for a usage example.

## Implementation note: `StaticLocationEngine`

If you dig around the FerrostarMapLibreUI modules, you may come across as `StaticLocationEngine`.

The static location engine exists to bridge between Ferrostar location providers and MapLibre.
MapLibre uses `LocationEngine` objects, not platform-native location clients, as its first line.
This is smart, since it makes MapLibre generic enough to support location from other sources.
For Ferrostar, it enables us to account for things like snapping, simulated routes, etc.
The easiest way to hide all that complexity from `LocationProvider` implementors
is to introduce the `StaticLocationEngine` with a simple interface to set location.

This is mostly transparent to developers using Ferrostar,
but in case you come across it, hopefully this note explains the purpose.
