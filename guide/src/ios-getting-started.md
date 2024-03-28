# Getting Started on iOS

This section of the guide covers how to integrate Ferrostar into an iOS app.
We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

TODO

## Add the Swift package dependency

## Configure location services

### Permissions and privacy strings

### Location providers

You'll need to configure a provider to get location updates.
We bundle a few implementations to get you started, or you can create your own.
The broad steps are the same regardless of which provider you use:
create an instance of the class,
store it in an instance variable where it makes sense,
and (if simulating a route) set the location manually or enter a simulated route.

The API is similar to the iOS location APIs you may already know,
and you can start or stop updates at will.

#### `SimulatedLocationProvider`

The `SimulatedLocationProvider` allows for simulating location within Ferrostar
without needing GPX files or complicated environment setup.
This is great for testing and development without stepping outside.

First, instantiate the class.
This is usually saved as an instance variable.

```swift
private let locationProvider = SimulatedLocationProvider(location: initialLocation)
```

You can set a new location using the `lastLocation` property at any time.
Optionally, once you have a route, simulate the replay of the route.
You can set a `warpFactor` to play it back faster.

```swift
locationProvider.warpFactor = 2
locationProvider.setSimulatedRoute(route)
```

#### `CoreLocationProvider`

The `CoreLocationProvider` provides a ready-to-go wrapper around a `CLLocationManager`
for getting location updates from GNSS.
It automatically takes care of requesting sensible permissions for you
(make sure you have permission strings set in your `Info.plist`!).

```swift
private let locationProvider = CoreLocationProvider(activityType: .automotiveNavigation)
```

## Configure the `FerrostarCore` instance

`FerrostarCore` automatically starts and stops the `LocationProvider` updates
along with `startNavigation(route:config:)` and `stopNavigation` calls.

## Getting a route

TODO

## Starting a navigation session

TODO

## Using the NavigationMapView

TODO

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/apple/DemoApp) with an example integration.

## Going deeper

This covers the basic “batteries included” configuration which works for simple apps.
But there’s a lot of room for customization!
Skip on over to the customization chapters that interest you.