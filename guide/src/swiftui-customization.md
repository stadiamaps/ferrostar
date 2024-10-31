# UI customization with SwiftUI

The [iOS tutorial](ios-getting-started.md) gets you set up with a “batteries included” UI
and sane defaults,
but this doesn’t work for every use case.
This page walks you through the ways to customize the SwiftUI frontend to your liking.

## Customizing the map

Ferrostar includes a map view built with the
[MapLibre SwiftUI DSL](https://github.com/maplibre/swiftui-dsl).
This is designed to be fairly configurable,
so if the existing customizations don’t work for you,
we’d love to hear why via an issue on GitHub!

In the case that you want complete control though,
the provided wrappers around map view are not that complex.

TODO: Docs on how to build your own navigation views + describe the current overlay layers.

The demo app is designed to be instructive in showing many available options,
so be sure to look at that to build intuition.

### Style

You can pass a style URL to any of the navigation map view constructors.
You can vary this dynamically as your app theme changes (ex: in dark mode).

### Camera

The camera supports two-way manipulation via SwiftUI bindings.
TODO: more documentation

### Adding map layers

You can add your own overlays too!
The `makeMapContent` closure argument of the various map and navigation views
enables you to add more layers.
See the demo app for an example, where we add a little dot showing the raw location
in addition to the puck, which snaps to the route line.

## Customizing the instruction banners

Ferrostar includes a number of views related to instruction banners.
These are composed together to provide sensible defaults,
but you can customize a number of things.

### Distance formatting

By default, banners and other UI elements involving distance will be formatted using an `MKDistanceFormatter`.

This should “just work” for most cases as it is aware of the device’s locale.
However, you can customize the formatting by passing in any arbitrary `Formatter`.
This can be your own specially configured `MKDistanceFormatter` or a custom subclass
which formats things to your liking.

### Banner instruction views

The `InstructionsView` is shipped with Ferrostar is the default banner view.
It uses the public domain directions iconography from Mapbox in a standard layout.

This view is an excellent example of composability, and is comprised of several subviews.
The units of the `InstructionsView` are controlled using the formatter settings
you passed to the `NavigationMapView` (if you’re using it).
If you’re not using the `NavigationMapView`, you can pass a formatter directly.

You can also build your own custom banners using the provided components,
or start from scratch.

TODO: Expose a view builder argument so that users can easily swap for their own view.

## Customizing the navigation view grid

The batteries included UI has a lot of widgets,
like zoom buttons and a status bar showing your ETA.

This grid is completely customizable!
You can add, move, or replace widgets from the defaults.

Refer to the `CustomizableNavigatingInnerGridView` public protocol and extension.
The `PortraitNavigationOverlayView` and `LandscapeNavigationOverlayView`
are complete overlay configuration examples.
Specifically, they are the ones used by default (in the `DynamicallyOrientingNavigationView`).
With this context, you should be able to see how the default views are composed
from others,
and design your own custom overlay configuration,
mixing the views provided in `FerrostarMapLibre` with your own!

## Customizing the high-level navigation views

The `DynamicallyOrientingNavigationView`
on both platforms also has a high level of flexibility via both constructor arguments and
(for SwiftUI) view modifiers.
We won't make an exhaustive list here,
but have a look at the documentation / code,
or explore in your IDE.

A few non-obvious notes for SwiftUI users are in order.
First, if you want to display speed limits,
make sure you use the `navigationSpeedLimit` view modifier
on your navigation view.
We may add a default in the future,
but which signage to use is somewhat tricky and perhaps app-specific.

Second, road names are included from your routes by default.
You can change the styling with view modifiers,
or replace it with your own view like so:

```swift
// DynamicallyOrientingNavigationView(...)
    .navigationCurrentRoadView(currentRoadNameViewBuilder: {
	    // TODO: Your view here, based on state...
		EmptyView()
	})
```

If you want to disable road names completely,
you can return `EmptyView()` as shown above.
