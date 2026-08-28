# Getting Started on iOS

This section of the guide covers how to integrate Ferrostar into an iOS app.
We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

## Add the Swift package dependency

If you’re not familiar with adding Swift Package dependencies to apps,
Apple has some helpful [documentation](https://developer.apple.com/documentation/xcode/adding-package-dependencies-to-your-app).
You can search for the repository via its URL:
`https://github.com/stadiamaps/ferrostar`.

Unless you are sure you know what you’re doing, you should use a tag (rather than a branch)
and update along with releases.
Since auto-generated bindings have to be checked in to source control
(due to how SPM works),
it’s possible to have intra-release breakage if you track `master`.

## Configure location services

To access the user’s real location,
you first need to set a key in your Info.plist or similar file.
This is something you can set in Xcode by going to your project,
selecting the target, and going to the Info tab.

You need to add row for “Privacy - Location When In Use Usage Description”
(right-click any of the existing rows and click “Add row”)
or, if you’re using raw keys, `NSLocationWhenInUseUsageDescription`.
Fill in a description of why your app needs access to their location.
Presumably something related to navigation ;)

### Location providers

You'll need to configure a provider to get location updates.
We bundle a few implementations to get you started, or you can create your own.
The broad steps are the same regardless of which provider you use:
create an instance of the class,
store it in an instance variable where it makes sense,
and (if simulating a route) set the location manually or enter a simulated route.

The API is similar to the iOS location APIs you may already know,
and you can start or stop updates at will.

You should store your location provider in a place that is persistent for as long as you need it.
Most often this makes sense as a private `@StateObject` if you’re using SwiftUI.

#### `CoreLocationProvider`

The `CoreLocationProvider` provides a ready-to-go wrapper around a `CLLocationManager`
for getting location updates from GNSS.
It will automatically request permissions for you as part of initialization.

```swift
@StateObject private var locationProvider = CoreLocationProvider(activityType: .otherNavigation, allowBackgroundLocationUpdates: true)
```

<div class="warning">

If you want to access the user’s location while the app is in the background,
you need to declare the location updates background mode in your `Info.plist`.
You can find more details [in the Apple documentation](https://developer.apple.com/documentation/corelocation/handling-location-updates-in-the-background).

</div>

#### `SimulatedLocationProvider`

The `SimulatedLocationProvider` allows for simulating location within Ferrostar
without needing GPX files or complicated environment setup.
This is great for testing and development without stepping outside.

First, instantiate the class.
This is usually saved as an instance variable.

```swift
@StateObject private var locationProvider = SimulatedLocationProvider(location: initialLocation)
```

You can set a new location using the `lastLocation` property at any time.
Optionally, once you have a route, simulate the replay of the route.
You can set a `warpFactor` to play it back faster.

```swift
locationProvider.warpFactor = 2
locationProvider.setSimulatedRoute(route)
```

## Configure the `FerrostarCore` instance

Next, you’ll want to create a `FerrostarCore` instance.
This is your interface into Ferrostar.
You’ll also want to keep this around as a persistent property
and use it in SwiftUI using a `private @StateObject` or `@ObservedObject`
in most situations.

`FerrostarCore` provides several initializers,
including convenience helpers for well-known route providers
and the ability to provide your own custom routing (ex: for local/offline use).

Here's a full example configuration:

```swift
let core = try FerrostarCore(
    wellKnownRouteProvider: .valhalla(
        endpointUrl: "https://api.stadiamaps.com/route/v1?api_key=\(sharedAPIKeys.stadiaMapsAPIKey)",
        profile: "bicycle"
    )
    .withJsonOptions(options: ["costing_options": ["bicycle": ["use_roads": 0.2]]]),
    locationProvider: locationProvider,
    navigationControllerConfig: config,
    // This is how you can set up annotation publishing;
    // We provide "extended OSRM" support out of the box,
    // but this is fully extendable!
    annotation: AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM(),
    widgetProvider: FerrostarWidgetProvider()
)
```

Let's dive into the configuration options a bit further.

### Route Providers

You’ll need a route provider when you set up your `FerrostarCore` instance.
Several are available via the `WellKnownRouteProvider` enum,
or you can [configure your own from scratch](./route-providers.md).
Refer to the [commercial vendors](./vendors.md) page for some known working integrations.

### Set up Voice Guidance

If your routes include spoken instructions,
Ferrostar can trigger the speech synthesis at the right time.
Ferrostar includes the `SpokenInstructionObserver` class, 
which can use `AVSpeechSynthesizer` or your own speech synthesis. This is the default for the `FerrostarCore` initializer.

The `SpeechSynthesizer` protocol
specifies the required interface,
and you can build your own implementation on this,
such as a local AI model or cloud service like Amazon Polly.
PRs welcome to add other publicly accessible speech API implementations.

Your navigation view can store the spoken instruction observer as an instance variable:

```swift
@State private var spokenInstructionObserver = SpokenInstructionObserver.initAVSpeechSynthesizer()
```

Then, you'll need to initialize `FerrostarCore` to reference it. As stated above, it has a default parameter to use `AVSpeechSynthesizer`.

Finally, you can use this to drive state on navigation view.
`DynamicallyOrientingNavigationView` has constructor arguments to configure the mute button UI.
See the demo app for an example.

### (Optional) Configure annotation parsing

Want to get speed limit information?
Or have your own live traffic layers?
Annotations provide a way to bring this information into your routes.
We support some standard ones out of the box.
Check out the chapter on [annotations](annotations.md) for details.

## Getting a route

Before getting routes, you’ll need the user’s current location.
You can get this from the location provider (which is part of why you’ll want to hang on to it).
Next, you’ll need a set of waypoints to visit.
Finally, you can use the asynchronous `getRoutes` method on `FerrostarCore`.
Here’s an example:

```swift
Task {
    do {
        routes = try await ferrostarCore.getRoutes(initialLocation: userLocation,
		                                           waypoints: [
												       Waypoint(coordinate: GeographicCoordinate(cl: loc.location), kind: .break)
												   ])

        errorMessage = nil
        
        // TODO: Let the user select a route, or pick one programmatically
    } catch {
        // Communicate the error to the user.
        errorMessage = "Error: \(error)"
    }
}
```

### Additional waypoint properties

The example above uses simple waypoints that will work with any routing engine.
But many routing engines, including Valhalla which we run at Stadia Maps,
let you provide additional detail.

Ferrostar supports this with engine-specific properties.
Refer to the [Route Providers documentation](./route-providers.md#bundled-support) for more details.

## Starting a navigation session

Once you or the user has selected a route, it’s time to start navigating!

```swift
// NOTE: You can also change your config here with an optional config parameter!
try ferrostarCore.startNavigation(route: route)
```

From this point, `FerrostarCore` automatically starts the `LocationProvider` updates,
and will use Combine, the SwiftUI observation framework, to publish state changes.

## Using the `DynamicallyOrientingNavigationView`

So now navigation is “started” but what does that mean?
Let’s turn these state updates into a familiar map-centric experience!

We’ll use the `DynamicallyOrientingNavigationView` together with a map style,
the state from the core, and many more configurable properties
See the class documentation for details on each parameter,
but you have the ability to customize most of the camera behavior.

```swift
// You can get a free Stadia Maps API key at https://client.stadiamaps.com
// See https://stadiamaps.github.io/ferrostar/vendors.html for additional vendors
let styleURL = URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(stadiaMapsAPIKey)")!
DynamicallyOrientingNavigationView(
    styleURL: styleURL,
    navigationState: state,
    camera: $camera,
    snappedZoom: .constant(18),
    useSnappedCamera: .constant(true))
```

## Preventing the screen from sleeping

If you’re navigating, you probably don’t want the screen to go to sleep.
You can prevent this by setting the `isIdleTimerDisabled` property on the `UIApplication.shared` object.

```swift
UIApplication.shared.isIdleTimerDisabled = true

// Don't forget to re-enable it when you're done!
UIApplication.shared.isIdleTimerDisabled = false
```

Refer to the [Apple documentation](https://developer.apple.com/documentation/uikit/uiapplication/1623070-isidletimerdisabled) for more information.

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/apple/DemoApp) with an example integration.

## Going deeper

This covers the basic “batteries included” configuration and pre-built UI.
But there’s a lot of room for customization!
Skip on over to the customization chapters that interest you.
