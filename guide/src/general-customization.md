# General Customizations

## Customizing `FerrostarCore` behaviors

`FerrostarCore` sits at the heart of your interactions with Ferrostar.
It provides an idiomatic way to interact with the core
from native code at the platform layer.

### Ferrostar `NavigationControllerConfig` options

These customizations apply to the [`NavigationController`](https://docs.rs/ferrostar/latest/ferrostar/navigation_controller/struct.NavigationController.html).
They are surfaced at the platform layer
as an argument to the `startNavigation` method of `FerrostarCore`.

#### Step advance

The step advance mode controls when navigation advances to the next step in the route.
See the `StepAdvanceMode` for details
(either in the [rustdocs](https://docs.rs/ferrostar/latest/ferrostar/navigation_controller/models/enum.StepAdvanceMode.html)
or in the equivalent platform library docs;
the API is bridged idiomatically to Swift and Kotlin).

Ferrostar currently includes several simplistic methods,
but more implementations are welcome!

You can use the manual step advance mode to disable automatic progress,
and trigger advance on your own using the `advanceToNextStep` method
on `FerrostarCore` at the platform layer.

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
