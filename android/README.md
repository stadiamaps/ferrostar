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

* `io.github.rallista:maplibre-compose` is no longer used by `ui-maplibre`.
* `NavigationMapView`, `PortraitNavigationView`, `LandscapeNavigationView`, and `DynamicallyOrientingNavigationView` now use a Ferrostar-owned `NavigationMapState` facade via `rememberNavigationMapState()`.
* The old `MapViewCamera`-based camera state has been replaced by a small Ferrostar camera layer for:
  * follow user
  * follow user with bearing
  * route overview
  * free camera
* `onMapReadyCallback` is still available on `NavigationMapView` for the 0.x series.
* Location puck styling is configurable through `NavigationMapPuckStyle`.
* Route rendering now uses a GeoJSON source plus `LineLayer` instead of legacy polyline convenience APIs.
* Map tap and long-press callbacks use Ferrostar-facing callbacks with `GeographicCoordinate` plus screen position.

Example migration for default usage:

```kotlin
val navigationMapState = rememberNavigationMapState()

DynamicallyOrientingNavigationView(
    modifier = Modifier.fillMaxSize(),
    styleUrl = styleUrl,
    navigationMapState = navigationMapState,
    viewModel = viewModel,
)
```

Custom camera control now goes through `NavigationMapState`:

```kotlin
navigationMapState.zoomIn()
navigationMapState.recenter(isNavigating = true)
navigationMapState.showRouteOverview(boundingBox, paddingValues = mapInsets)
```

Current scope notes:

* This migration covers Android phone/tablet Compose only.
* Android Auto remains out of scope for this issue; the legacy car-specific path is kept separately so the repo still builds.
