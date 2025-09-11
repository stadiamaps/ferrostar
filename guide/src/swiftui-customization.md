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

### Replacing the route polyline overlay

Ferrostar includes a default route polyline overlay that is pre-styled. You can replace it
using the `navigationMapViewRouteOverlay()` modifier. See `RouteStyleLayer` for the default
polyline for reference.

```swift
            .navigationMapViewRouteOverlay { state in
                if let routeGeometry = state?.routeGeometry {
                    RouteStyleLayer(
                        polyline: MLNPolylineFeature(coordinates: routeGeometry.map(\.clLocationCoordinate2D)),
                        identifier: "route-polyline",
                        style: MyCustomRouteStyle()
                    )
                }
            }
```

### Customizing the Map View's Content Inset

The content inset is used on the Ferrostar NavigationMapView (and MapLibre's SwiftUI DSL MapView) to control
the padding around the center of the map. This is useful for moving the user's puck to the bottom of the screen
and when landscape, to the trailing half.

Ferrostar includes the basic raw content inset modifier as well as some advanced landscape and portrait modifiers
that take the view's actual height and apply a percentage. See `NavigationMapViewContentInsetMode`.

The content inset can be customized using the `navigationMapViewContentInset(...)` modifiers.

The content inset can accessed using the `@Environment(\.navigationMapViewContentInsetConfiguration)`
environment property.

## Customizing the Navigation Views

The batteries included UI has a lot of widgets, like zoom buttons and a status bar
showing your ETA. We've also provided several ways to modify the appearance of these views.

### Distance formatting

By default, banners and other UI elements involving distance will be formatted using an `MKDistanceFormatter`.

This should “just work” for most cases as it is aware of the device’s locale.
However, you can customize the formatting by passing in any arbitrary `Formatter`.
This can be your own specially configured `MKDistanceFormatter` or a custom subclass
which formats things to your liking.

String formatters can be customized by providing a custom `FormatterCollection` using
`navigationFormatterCollection(_:)` View modifier.

The formatter collection can be accessed using the `@Environment(\.navigationFormatterCollection)`
environment property.

### Banner Instructions View, Trip Progress View and Current Road Name View

The `InstructionsView` is shipped with Ferrostar is the default banner view.
It uses the public domain directions iconography from Mapbox in a standard layout.

This view is an excellent example of composability, and is comprised of several subviews.
The units of the `InstructionsView` are controlled using the formatter settings
you passed to the `NavigationMapView` (if you’re using it).
If you’re not using the `NavigationMapView`, you can pass a formatter directly.

The `TripProgressView` included in Ferrostar includes a basic layout showing the
progress of the trip, formatted with the specified or default `FormatterCollection`.

The `CurrentRoadNameView` is a route colored label that specifies the name of the road
the puck is currently on. It's styled to match the default Ferrostar route style.

All of these views can be replaced using their view modifier extensions.

```swift
    DynamicallyOrientingNavigationView(...)
        .navigationViewInstructionView { navigationState, isExpanded, sizeWhenNotExpanded in
            MyCustomTopCenterView(navigationState, isExpanded, sizeWhenNotExpanded)
        }
        .navigationViewProgressView { navigationState, onTapExit in
            MyCustomProgressView(navigationState, onTapExit)
        }
        .navigationViewCurrentRoadNameView { navigationState in
            MyCustomCurrentRoadNameView(navigationState)
        }
```

If you want to disable road names completely, you can return `EmptyView()` as shown above.

```swift
    DynamicallyOrientingNavigationView(...)
        .navigationCurrentRoadView { _ in
    		EmptyView()
    	}
```

These views can be accessed using the `@Environment(\.navigationViewComponentsConfiguration)` environment value.
However, it's also likely if you were building a custom navigation view that you'd just build it with your own custom views.

### Speed Limit Views

> [!IMPORTANT]
> This is opt-in only.

If your route provider includes speed limits, you can use the `navigationSpeedLimit` view modifier extension.

```swift
    DynamicallyOrientingNavigationView(...)
        .navigationSpeedLimit(
            speedLimit: speedLimitMeasurement,
            speedLimitStyle: .viennaConvention // Vienna convention = most of the world; you can use .mutcdStyle for the US style
        )
```

The speed limit and speed limit style can be accessed using `@Environment(\.speedLimitConfiguration)`

### Adding Views to the Inner Grid

The Inner Grid is a series of rectangle views that fill the area on the navigation view between the
instructions bar and the progress view (or the right of the screen when landscape). This view allows
easily adding widgets like buttons, labels, etc. Certain areas are already in use for included buttons
and others are blocked like the center of the view.

```swift
    DynamicallyOrientingNavigationView(...)
        .navigationViewInnerGrid(
            topCenter: {
                MyCustomTopCenterView()
            },
            // ... Customize any or all of the others (topTrailing, midLeading, bottomLeading, bottomTrailing)
        )
```

The views can be accessed using `@Environment(\.navigationInnerGridConfiguration)`. However, if you're building
a custom NavigationView, you'd probably just use the `InnerGridView` directly.
