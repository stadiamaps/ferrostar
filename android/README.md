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

## Baseline Profiles

The `baselineprofile` module generates baseline profiles for the `demo-app` to improve startup and runtime performance.

To verify the build compiles:

```shell
./gradlew :demo-app:assembleBenchmark
```

To generate baseline profiles (requires a connected device or emulator):

```shell
./gradlew :demo-app:generateBaselineProfile
```

Or use the convenience script from the android directory:

```shell
./scripts/baseline_profiler/generate_baseline_profiler.sh
```

## Testing locally in a separate project

* Bump the version number to a `SNAPSHOT` in `build.gradle`.
* run `./gradlew publishToMavenLocal -Pskip.signing`
* reference the updated version number in the project, and ensure that `mavenLocal` is one of the `repositories`.
