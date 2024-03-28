# SwiftUI

The tutorial get you set up with defaults using a “batteries included” UI,
but realistically this doesn’t work for every use case.
This page walks you through the ways you can customize things to your liking.

Note that this page is very much WIP.

## Customizing the map

Ferrostar includes a `NavigationMapView` based on [MapLibre Native](https://maplibre.org/).
This is configurable with a number of constructor parameters.
If the existing customizations don’t work for you,
first we’d love to hear why via an issue on GitHub!
In the case that you want complete control though,
the map view itself is actually not that complex.

## Customizing the instruction banners

Ferrostar includes a number of views related to instruction banners.
These are composed together to provide sensible defaults,
but you can customize a number of things.

### Style

TODO: We allow you to pass a light and dark style URL.
This is ~~broken~~ very much WIP and is likely to change ;)

### Camera

TODO: Ability to override the built-in camera behavior (probably define a protocol for this).

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