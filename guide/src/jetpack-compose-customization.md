# UI customization with Jetpack Compose

The tutorial get you set up with defaults using a “batteries included” UI,
but realistically this doesn’t work for every use case.
This page walks you through the ways to customize the Compose UI to your liking.

Note that this section is very much WIP.

## Customizing the map

Ferrostar includes a `NavigationMapView` based on [MapLibre Native](https://maplibre.org/).
This is configurable with a number of constructor parameters.
If the existing customizations don’t work for you,
first we’d love to hear why via an issue on GitHub!
In the case that you want complete control though,
the map view itself is actually not that complex.

### Style

The demo app uses the MapLibre demo tiles, but you’ll need a proper basemap eventually.
Just pass in the URL of any MapLibre-compatible JSON style.
See the [vendors page](./vendors.md) for some ideas.

### Camera

TODO: Docs on how this works.

### Adding map layers

You can add your own overlays to the map as well (any class, including `DynamicallyOrientingNavigationView`)!
The `content` closure argument lets you add more layers.
See the demo app for an example.

### Styling the route polyline

You can customize or replace the built-in route polyline rendering with the `routeOverlayBuilder` parameter
on all MapLibre composable views.
Here's an example with `DynamicallyOrientingNavigationView`:

```kotlin
  DynamicallyOrientingNavigationView(
      modifier = Modifier.fillMaxSize(),
      styleUrl = AppModule.mapStyleUrl,
      camera = camera,
      viewModel = viewModel,
      routeOverlayBuilder = RouteOverlayBuilder(
        navigationPath = { uiState ->
          uiState.routeGeometry?.let { geometry ->
		    // BorderedPolyline is part of Ferrostar;
			// you can also drop down to the raw Polyline and build your own custom style.
            BorderedPolyline(points = geometry.map { LatLng(it.lat, it.lng) }, zIndex = 0, color = "#3583dd33")
          }
        }),
	  // ...
```

## Configuring visual elements of the composable map views

You can configure which controls appear in our MapLibre composable views with the `config` parameter.
Here's an example with `DynamicallyOrientingNavigationView`:

```kotlin
  DynamicallyOrientingNavigationView(
      modifier = Modifier.fillMaxSize(),
      styleUrl = AppModule.mapStyleUrl,
      camera = camera,
      viewModel = viewModel,
      config = VisualNavigationViewConfig(showMute = true, showZoom = false, showRecenter = true, speedLimitStyle = SignageStyle.MUTCD),
      // ...
```

You can also customize or replace the instruction banners, trip progress view, and road name overlays with your own composables.
These are configurable via the `NavigationComponentBuilder`.
Instruction banners in particular have a lot of built-in configurablity, which we'll talk about in the next section.

## Customizing the instruction banners

Ferrostar includes a number of views related to instruction banners.
These are composed together to provide sensible defaults,
but you can customize a number of things.

### Distance formatting

By default, banners and other UI elements involving distance will be formatted using the bundled `com.stadiamaps.ferrostar.composeui.LocalizedDistanceFormatter`.
Distance formatting is a complex topic though, and there are ways it can go wrong.

The Android ecosystem unfortunately does not include a generalized distance formatter,
so we have to roll our own.
Java locale does not directly specify which units should be preferred for a class of measurement.

We attempt to infer the correct measurement system to use,
using some newer Android APIs.
Unfortunately, Android doesn’t really have a facility for specifying measurement system
independently of the language and region setting.
So, we allow overrides in our default implementation.

If this isn’t enough, you can implement your own formatter
by implementing the `com.stadiamaps.ferrostar.composeui.DistanceFormatter` interface.

If you find an edge case, please file a bug report (and PR if possible)!

### Banner instruction composables

The `com.stadiamaps.ferrostar.composeui.views.InstructionsView` composable
comes with sensible defaults, with plenty of override hooks.
The default behavior is to use Mapbox’s public domain iconography,
format distances using the device’s locale preferences,
and use a color scheme and typography based on the Material theme.

You can pass a customized distance formatter as noted above,
and you can also override the theme directly if you’d like
more control than our defaults derived from the Material theme.

Finally, you can override the leading edge content.
Just write your own composable content block rather than accept the default.

If you need even more control, you can use the `com.stadiamaps.ferrostar.composeui.views.maneuver.ManeuverInstructionView` directly,
or write your own, optionally using the `ManeuverImage`.
