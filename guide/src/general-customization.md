# General Customizations

## Customizing `FerrostarCore` behaviors

`FerrostarCore` sits at the heart of your interactions with Ferrostar.
It provides an idiomatic way to interact with the core
from native code at the platform layer.

### Ferrostar `NavigationControllerConfig` options

These customizations apply to the [`NavigationController`](https://docs.rs/ferrostar/latest/ferrostar/navigation_controller/struct.NavigationController.html).
They are surfaced at the platform layer
as an argument to the `startNavigation` method of `FerrostarCore`.

### Waypoint advance

The waypoint advance is configured by the `WaypointAdvanceMode`. Our original (V0.27.0 and earlier) hardcoded implementation
simply checked if the user was within 100 meters of the waypoint when the step was being advanced. The waypoint advance logic
is separated from step advance to ensure that the user will advance waypoint regardless of step advance conditions.

To match Ferrostar's original default, use `WaypointAdvanceMode::WaypointWithinRange(100.0)`.

The waypoint advance mode has access to the entire TripState anytime the user location is updated. As a result more complex
implementations would be welcome additions to the library.

#### Step advance

The step advance system uses a common `StepAdvanceCondition` trait that tells the NavigationController
whether to advance the step or not on every iteration (user location update). Several implementations
are provided:

| `StepAdvanceCondition | Methodology | Description |
| --- | --- | --- |
| `ManualStepCondition` | Never | This is useful for conditions where you may want the user to manually call the navigation controller's advance method. |
| `DistanceToEndOfStepCondition` | Automatically | Eagerly advance when the user is within range of the step's end. |
| `DistanceFromStepCondition` | Automatically | Delay the advance until the user is a distance from the step. |
| `OrAdvanceConditions` | Automatically | Combine multiple conditions with an OR operation. |
| `AndAdvanceConditions` | Automatically | Combine multiple conditions with an AND operation. |
| `DistanceEntryAndExitCondition` | Automatically | Advance when the user enters a range of the step's end and wait for the user to move away from the step. |

If you want a new condition, the trait is relatively simple to implement and PR's are welcome!

The customizable step advance conditions are designed to help you create more complex and flexible step advancement logic. You can also combine multiple conditions using the `OrAdvanceConditions` and `AndAdvanceConditions`.

##### Recommended conditions

The `DistanceEntryAndExitCondition` is ideal for most navigation use cases, but may be overkill in certain cases.
It's primary focus is on ensuring the user has approximately reached the end of the step, and then intentionally
proceeded past it. This is particularly useful for scenarios like stop lights, where the simpler `DistanceToEndOfStepCondition`
would appear to make the maneuver before the user has actually done so in real life.

The `OrAdvanceConditions` may be useful to attach a backup condition to ensure the user doesn't get stuck when offline. E.g.
the user may have lost connection and missed a step's end only to then re-joined the route. In a situation like this you
could add an Or with `DistanceFromStepCondition` with a very large value to effectively clean up a stuck state while offline.

#### Route deviation tracking

Ferrostar recognizes that there is no one-size-fits-all solution
for determining when a user has deviated from the intended route.
The core provides several configurable detection strategies,
but you can also bring your own.
(And yes, you *can* write your custom deviation detection code in
your native mobile codebase.)

See the [`RouteDeviationTracking` rustdocs](https://docs.rs/ferrostar/latest/ferrostar/deviation_detection/enum.RouteDeviationTracking.html)
for the list of available strategies and parameters.
(The Rust API is bridged idiomatically to Swift and Kotlin.)

### Configuring a `RouteDeviationHandler`

By default, Ferrostar will fetch new routes when the user goes off course.
You can override this behavior with hooks on `FerrostarCore`.

On iOS, all customization happens via the delegate.
Implement the `core:correctiveActionForDeviation:remainingWaypoints` method
on your delegate and you’re all set.
On Android, set the `deviationHandler` property.

Refer to the demo apps for custom implementation examples.
Note that you can disable the default behavior of attempting to reroute
by setting an empty implementation
(not setting the property to `nil`/`null`!).

### Alternate route handling

`FerrostarCore` may occasionally load alternative routes.
At this point, this is only used for recalculating when the user deviates from the route,
but it may be used for additional behaviors in the future,
such as speculative route checking based on current traffic (for supported vendors).

By default, Ferrostar will “accept” the new route
and reroute the active navigation session
whenever a new route arrives *while the user is off course*.

On iOS, you can configure this behavior by adding a delegate to the core
and implementing the `core:loadedAlternateRoutes` method.
On Android, you can configure this behavior by providing an `AlternateRouteProcessor`
implementation to `FerrostarCore`.

Refer to the demo apps examples of custom implementations.

Note that you can disable the default behavior of attempting to reroute
by setting an empty implementation (*not* setting the property to `nil`/ `null`!).

## Customizing prompt timings

While most APIs don’t offer customizable timing for banners and spoken prompts,
you can edit the standardized `Route` responses directly!
A functional `map` operation drilling down to the instructions
is an elegant way to change trigger distances.
