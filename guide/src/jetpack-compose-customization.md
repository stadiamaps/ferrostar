# UI customization with Jetpack Compose

The tutorial gets you set up with defaults using a “batteries included” UI,
but realistically this doesn’t work for every use case.
This page walks you through the ways to customize the Compose UI to your liking.

Note that this section is very much WIP.

## MapLibre Compose migration notes

The Android MapLibre UI now uses the official
[MapLibre Compose](https://github.com/maplibre/maplibre-compose) `org.maplibre.compose` APIs
instead of the previous Rallista APIs. The most important integration changes are:

- pass `baseStyle: BaseStyle` instead of a plain style URL
- create and pass a `NavigationMapState` with `rememberNavigationMapState()`
- use `MapOptions` for map, ornament, gesture, and render options
- add app-specific sources and layers through the `content` slot with normal
  [MapLibre Compose layer APIs](https://maplibre.org/maplibre-compose/layers/)
- use `navigationMapState.cameraState` for projection queries and imperative camera animations
- disable Ferrostar's default route or puck with `routeOverlayBuilder = null` and
  `showDefaultPuck = false` when drawing custom map content

If you call `navigationMapState.cameraState.animateTo(...)` directly, switch to
`NavigationCameraMode.FREE` first so the tracking camera does not overwrite your custom animation.

## Customizing the map

Ferrostar includes a `NavigationMapView` based on [MapLibre Native](https://maplibre.org/).
This is configurable with a number of constructor parameters.
If the existing customizations don’t work for you,
first we’d love to hear why via an issue on GitHub!
In the case that you want complete control though,
the map view itself is actually not that complex.

### Style

The demo app uses the MapLibre demo tiles, but you’ll need a proper basemap eventually.
Wrap the URL in `BaseStyle.Uri(...)`, or pass `BaseStyle.Json(...)` if you generate
style JSON in memory.
See the [vendors page](./vendors.md) for some ideas.

```kotlin
  val navigationMapState = rememberNavigationMapState()

  NavigationMapView(
      baseStyle = BaseStyle.Uri(styleUrl),
      navigationMapState = navigationMapState,
      uiState = uiState,
      mapOptions = MapOptions(),
  )
```

If you already manage styles yourself, `BaseStyle.Json(styleJson)` works as well.

### Camera

Ferrostar's Jetpack Compose views provide several forms of camera configuration.
`NavigationMapState` owns the mutable camera state for `NavigationMapView` and the built-in
phone/tablet wrapper views. It supports Ferrostar's follow/recenter/overview helpers, and its
public `cameraState` gives you direct access to MapLibre Compose camera APIs for custom animation
and projection queries.

```kotlin
  val navigationMapState = rememberNavigationMapState()

  DynamicallyOrientingNavigationView(
      modifier = Modifier.fillMaxSize(),
      baseStyle = BaseStyle.Uri(AppModule.mapStyleUrl),
      navigationMapState = navigationMapState,
	  // ...
```

```kotlin
  navigationMapState.zoomIn()
  navigationMapState.recenter(isNavigating = true)
  navigationMapState.showRouteOverview(boundingBox, paddingValues = mapInsets)

  // For app-specific camera control, switch to FREE mode first so tracking
  // does not immediately overwrite your custom camera animation.
  navigationMapState.cameraMode = NavigationCameraMode.FREE
  navigationMapState.cameraState.animateTo(finalPosition = customPosition)

  navigationMapState.cameraMode = NavigationCameraMode.FREE
  navigationMapState.cameraState.animateTo(
      boundingBox = bounds,
      padding = mapInsets,
  )
```

### Adding map layers

You can add your own overlays to the map on any Ferrostar MapLibre composable view.
The `content` closure runs inside the underlying `MaplibreMap`, so you can use the
normal [MapLibre Compose source and layer APIs](https://maplibre.org/maplibre-compose/layers/)
there.

```kotlin
  val pinJson = """
    {"type":"FeatureCollection","features":[
      {"type":"Feature","geometry":{"type":"Point","coordinates":[16.3738,48.2082]},"properties":{}}
    ]}
  """.trimIndent()

  DynamicallyOrientingNavigationView(
      modifier = Modifier.fillMaxSize(),
      baseStyle = BaseStyle.Uri(AppModule.mapStyleUrl),
	  // Other arguments elided...
  ) { uiState ->
    val pointSource = rememberGeoJsonSource(GeoJsonData.JsonString(pinJson))

    CircleLayer(
        id = "custom-pin",
        source = pointSource,
        color = const(Color.Green),
        radius = const(12.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(3.dp),
    )
  }
```

If you need complete control over route lines, selection layers, pins, or a custom puck,
you can combine `content` with:

- `routeOverlayBuilder = null`
- `showDefaultPuck = false`

### Styling the route polyline

You can customize or replace the built-in route polyline rendering with the `routeOverlayBuilder` parameter
on all MapLibre composable views.
Here's an example with `DynamicallyOrientingNavigationView`:

```kotlin
  DynamicallyOrientingNavigationView(
      modifier = Modifier.fillMaxSize(),
      baseStyle = BaseStyle.Uri(AppModule.mapStyleUrl),
      viewModel = viewModel,
      routeOverlayBuilder = RouteOverlayBuilder(
        navigationPath = { uiState ->
          uiState.routeGeometry?.let { geometry ->
            // BorderedPolyline is part of Ferrostar's MapLibre UI package.
            // You can also drop down to raw layers and build your own custom style.
            BorderedPolyline(
                points = geometry,
                color = Color(0xFF3583DD),
                opacity = 0.7f,
                borderOpacity = 0.3f,
            )
          }
        }),
	  // ...
```

To disable Ferrostar's default route rendering entirely and draw your own route in `content`:

```kotlin
  NavigationMapView(
      baseStyle = BaseStyle.Uri(styleUrl),
      navigationMapState = navigationMapState,
      uiState = uiState,
      mapOptions = MapOptions(),
      routeOverlayBuilder = null,
  ) { uiState ->
      // Draw your own route layers here.
  }
```

### Customizing or disabling the puck

The built-in puck can be disabled if you want to render your own location indicator in `content`:

```kotlin
  NavigationMapView(
      baseStyle = BaseStyle.Uri(styleUrl),
      uiState = uiState,
      mapOptions = MapOptions(),
      showDefaultPuck = false,
  ) { uiState ->
      // Draw your own puck, route, selection, or other app-specific layers here.
  }
```

## Configuring visual elements of the composable map views

You can configure which controls appear in our MapLibre composable views with the `config` parameter.
Here's an example with `DynamicallyOrientingNavigationView`:

```kotlin
  DynamicallyOrientingNavigationView(
      modifier = Modifier.fillMaxSize(),
      baseStyle = BaseStyle.Uri(AppModule.mapStyleUrl),
      viewModel = viewModel,
      config = VisualNavigationViewConfig(showMute = true, showZoom = false, showRecenter = true, speedLimitStyle = SignageStyle.MUTCD),
      // ...
```

You can also customize or replace the instruction banners, trip progress view, and road name overlays with your own composables.
These are configurable via the `NavigationViewComponentBuilder`.
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
