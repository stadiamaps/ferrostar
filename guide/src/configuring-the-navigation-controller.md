# Configuring Navigation Behavior

Not all navigation experiences should behave the same,
so Ferrostar lets you customize many important aspects of navigation.

These options are surfaced when calling `startNavigation` on most platforms.
The higher-level platform interfaces wrap [`NavigationControllerConfig`](https://docs.rs/ferrostar/latest/ferrostar/navigation_controller/models/struct.NavigationControllerConfig.html) in the Rust core.

## `StepAdvanceMode`

The step advance mode describes when a maneuver is “complete”
and navigation should advance to the next step.
We have a few built-in variants in the core,
which you can find in the [Rust documentation](https://docs.rs/ferrostar/latest/ferrostar/navigation_controller/models/enum.StepAdvanceMode.html).
The high-level platform wrappers also have this and should show in your IDE documentation panel.

If you want to build your own custom step advance logic,
set the `StepAdvanceMode` to manual,
and observe the `TripState` in your application code.
Then, you can manually call `advanceToNextStep` on the `NavigationController`.

## `RouteDeviationTracking`

This determines when the user is off the route.
Certain applications (pedestrian navigation, for example) may want to disable this.

If the built-in deviation tracking options aren’t enough
(for example, if you want to do local map matching),
you can decide this yourself by implementing the `RouteDeviationDetector` interface.

PRs are welcome for improvements or new general-purpose behaviors.
You can also implement the interfaces directly in your Swift or Kotlin code!
Here are some trivial examples.

Swift:

```swift
let config = SwiftNavigationControllerConfig(
    stepAdvance: .relativeLineStringDistance(minimumHorizontalAccuracy: 16, automaticAdvanceDistance: 16),
    routeDeviationTracking: .custom(detector: { _, _, _ in
        // Pretend that the user is always off route
        .offRoute(deviationFromRouteLine: 42)
    }),
    snappedLocationCourseFiltering: .raw
)

try core.startNavigation(route: route, config: config)
```

Kotlin:

```kotlin
val config = NavigationControllerConfig(
    stepAdvance = StepAdvanceMode.RelativeLineStringDistance(16U, 16U),
    routeDeviationTracking =
        RouteDeviationTracking.Custom(
            detector =
                object : RouteDeviationDetector {
                  override fun checkRouteDeviation(
                      location: UserLocation,
                      route: Route,
                      currentRouteStep: RouteStep
                  ): RouteDeviation {
                    // Pretend that the user is always off route
                    return RouteDeviation.OffRoute(42.0)
                  }
                }),
    CourseFiltering.RAW)
core.startNavigation(route, config)
```

### Recalculation

NOTE: This section is currently specific to Swift and Kotlin.
The Rust core does not expose any primitives for handling recalculation;
this is currently at the platform level, and is not yet implemented for web.

The *default* behavior on supported platforms
is to recalculate whenever the core determines that the user is off the route.
Skip to the next section if the default behavior works for you.

If you want to do something more advanced though, you can!
In Ferrostar, **determining whether the user is off route and whether to recalculate the route are two separate concerns.**
Keep this in mind when writing your custom deviation detector.
For example, if you want to display a flashing red overlay
but not recalculate immediately,
you could immediately report the user as off route, but delay recalculation.

#### Interfaces for signaling when to recalculate

To reflect these separate responsibilities,
you can set a delegate (`FerrostarCoreDelegate`) on iOS
or `RouteDeviationHandler` on Android.
This lets you tell the core what corrective action (if any)
to take when the user deviates from the route.

To initiate recalculation, return an appropriate `CorrectiveAction`.
The higher-level platform layer will automatically handle the details
of making a new route request (ex: over HTTP),
ensuring that multiple parallel requests are not sent while waiting for a response,
and so on.
Your decision as an implementer of the interface is easy and narrowly defined;
the platform takes care of the rest unless you’re using the core directly
without a high-level wrapper.

## Interfaces for handling alternative routes

Closely related to recalculation due to going off route is alternative route handling.
This can occur either because you missed a turn and went off the route,
or for other reasons like live traffic info suggesting that
the current route is no longer optimal.
Both scenarios are handled via alternative route hooks.

As usual, Ferrostar tries to have sensible defaults.

Considering the recalculation case,
if you don’t specify custom behavior,
the platform layer (again, currently iOS and Android only)
will automatically start a new navigation session
with the first route it receives after recalculation.
As a sanity check,
this behavior only triggers if the user is *still* off-course
(if the user went back on track in the interim, nothing happens).
If you want to customize this behavior,
set a `FerrostarCoreDelegate` on iOS or an `AlternativeRouteProcessor` on Android.

So, what about other cases besides recalculation?
We envision live traffic, incidents, etc. being used to feed periodic
“route revalidation” in the future.
The same “alternative route” notification mechanism
can be extended (ex: with a reason why the alternative is being supplied)
for this purpose.