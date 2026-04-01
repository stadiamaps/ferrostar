# Ferrostar Android

This directory tree contains the Gradle workspace for Ferrostar on Android.

* `composeui` - Jetpack Compose UI elements which are not tightly coupled to any particular map renderer.
* `core` - The core module is where all the "business logic", location management, and other core functionality lives.
* `demo-app` - A minimal demonstration app.
* `google-play-services` - Optional functionality that depends on Google Play Services (like the fused location client wrapper). This is a separate module so that apps are able to "de-Google" if necessary.
* `maplibreui` - Map-related user interface components built with MapLibre.

## Running the demo app

To run the demo app, you'll need a Stadia Maps API key
(free for development and evaluation use; no credit card required; get one at https://client.stadiamaps.com/).
You can also modify it to work with your preferred maps and routing vendor by editing `AppModule.kt`.

Set your API key in `local.properties` to run the demo app
(it is functional with demo tiles and routing, but only for limited testing):

```properties
stadiaApiKey=YOUR-API-KEY
```

Or, to use GraphHopper for the routing:

```properties
graphhopperApiKey=YOUR-API-KEY
```

## Snapshot tests

We use Paparazzi for UI snapshot testing.
To update the snapshots, run `./gradlew recordPaparazziDebug`.

## Testing locally in a separate project

* Bump the version number to a `SNAPSHOT` in `build.gradle`.
* run `./gradlew publishToMavenLocal -Pskip.signing`
* reference the updated version number in the project, and ensure that `mavenLocal` is one of the `repositories`.

## MapLibre Compose Migration

`ui-maplibre` now targets the official MapLibre Compose Android artifact:

```kotlin
implementation("org.maplibre.compose:maplibre-compose-android:0.12.1")
```

Notable Android phone/tablet migration changes:

* `ui-maplibre` no longer uses `io.github.rallista:maplibre-compose`.
* `NavigationMapView`, `PortraitNavigationView`, `LandscapeNavigationView`, and `DynamicallyOrientingNavigationView` now use a Ferrostar-owned `NavigationMapState` via `rememberNavigationMapState()`.
* The old `MapViewCamera`-based camera API has been replaced by a small Ferrostar camera layer for:
  * follow user
  * follow user with bearing
  * route overview
  * free camera
* `NavigationMapView` now takes `MapOptions` instead of the old `MapControls` API.
* Location puck styling is configurable through `NavigationMapPuckStyle`.
* Route rendering now uses a GeoJSON source plus `LineLayer` instead of legacy polyline convenience APIs.
* Map tap and long-press callbacks use Ferrostar-facing callbacks with `GeographicCoordinate` plus screen position.
* `NavigationMapView` now exposes `onMapLoadFinished` and `onMapLoadFailed` instead of a native-style `onMapReadyCallback`.
* `NavigationMapView` and the phone/tablet wrapper views now take `baseStyle: BaseStyle` directly.
* `NavigationMapState.cameraState` is public for direct access to projection queries and imperative camera animation.
* Default route and puck rendering can be disabled with `routeOverlayBuilder = null` and `showDefaultPuck = false`.

Example usage:

```kotlin
val navigationMapState = rememberNavigationMapState()

DynamicallyOrientingNavigationView(
    modifier = Modifier.fillMaxSize(),
    baseStyle = BaseStyle.Uri(styleUrl),
    navigationMapState = navigationMapState,
    viewModel = viewModel,
)
```

Using a `BaseStyle` directly:

```kotlin
val navigationMapState = rememberNavigationMapState()

NavigationMapView(
    baseStyle = BaseStyle.Uri(styleUrl),
    navigationMapState = navigationMapState,
    uiState = uiState,
    mapOptions = MapOptions(),
    routeOverlayBuilder = null,
    showDefaultPuck = false,
)
```

If you generate style JSON in memory, `BaseStyle.Json(styleJson)` works as well.

Custom camera control now goes through `NavigationMapState`:

```kotlin
navigationMapState.zoomIn()
navigationMapState.recenter(isNavigating = true)
navigationMapState.showRouteOverview(boundingBox, paddingValues = mapInsets)
navigationMapState.cameraState.animateTo(finalPosition = cameraPosition)
navigationMapState.cameraState.animateTo(boundingBox = bounds, padding = mapInsets)
```

Current scope notes:

* This migration covers Android phone/tablet Compose first.
* `ui-maplibre-car-app` still exists as a compatibility path, but Android Auto has not been fully migrated to the new map state/camera model yet.
