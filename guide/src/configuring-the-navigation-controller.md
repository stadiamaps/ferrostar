# Configuring Navigation Behavior

Not all navigation experiences should behave the same,
so Ferrostar lets you customize many important aspects of navigation.

These options are surfaced when calling `startNavigation` on most platforms.
The higher-level platform interfaces wrap the `NavigationControllerConfig` in the Rust core.

## `StepAdvanceMode`

The step advance mode describes when a maneuver is “complete”
and navigation should advance to the next step.
We have a few built-in variants in the core,
which you can find in the [Rust documentation](https://docs.rs/ferrostar/latest/ferrostar/navigation_controller/models/enum.StepAdvanceMode.html).
The high-level platform wrappers also have this and should show in your IDE documentation panel.

If you want to build your own custom step advance logic, you can observe the `TripState`
in your application code and manually call `advanceToNextStep` on the `NavigationController`.

## `RouteDeviationTracking`

This determines when the user is off the route.
Certain applications (pedestrian navigation, for example) may want to disable this.

If the built-in deviation tracking options aren’t enough
(for example, if you want to do map matching),
you can decide this yourself by implementing the `RouteDeviationDetector` interface.
You can do this directly in your Swift or Kotlin code!

### Recalculation

Recalculation is separate, but related to whether the user is off the route.
If you provide your own deviation detector, keep this in mind!
Some apps may wish to display a flashing red overlay, for example, but not recalculate immediately.

NOTE: The default behavior is to recalculate whenever the core determines that the user is off the route.
You can skip to the next section if the default behavior works for you.

#### Interfaces for signaling when to recalculate

To reflect these separate responsibilities,
you can set a delegate (`FerrostarCoreDelegate`) on iOS
or `RouteDeviationHandler` on Android.
This lets you specify what corrective action to take when the user deviates from the route.

To initiate recalculation, return an appropriate `CorrectiveAction`.
The higher-level platform layer will automatically handle the details
of making a new route request (ex: over HTTP),
ensuring that multiple parallel requests are not sent while waiting for a response,
and so on.
Your decision as an implementer of the interface is easy and narrowly defined;
the platform takes care of the rest unless you’re using the core directly
without a high-level wrapper.

## Interfaces for handling alternative routes

If you’re following closely, you may have noticed that this section is a higher heading level.
What’s up with that?

Well, alternative route handling is potentially broader than just recalculations after missing a turn!
But let’s discuss that case first.

As usual, Ferrostar tries to have sensible defaults,
so if you don’t specify custom behavior,
the platform layer will automatically start a new navigation session
with the first route it finds after recalculation
*if* the user is still off-course (if the user is back on track, it is discarded).
If you want to customize this behavior,
set a `FerrostarCoreDelegate` on iOS or an `AlternativeRouteProcessor` on Android.
Be sure to handle edge cases like the user going back on the route!

So, what about other cases besides recalculation?
While the interfaces don’t yet exist to build this,
we envision some cases with live traffic benefiting from periodic
“route revalidation” based on current conditions.
The same “alternative route” notification mechanism
will accommodate this case.