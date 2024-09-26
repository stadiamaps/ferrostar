# Ferrostar Android

This directory tree contains the Gradle workspace for Ferrostar on Android.

* `composeui` - Jetpack Compose UI elements which are not tightly coupled to any particular map renderer.
* `core` - The core module is where all the "business logic", location management, and other core functionality lives.
* `demo-app` - A minimal demonstration app.
* `google-play-services` - Optional functionality that depends on Google Play Services (like a fused location client wrapper). This is a separate module so that apps are able to "de-Google" if necessary.
* `maplibreui` - Map-related user interface components built with MapLibre.

## Running the demo app

To run the demo app, you'll need a Stadia Maps API key
(free for development and evaluation use; no credit card required; get one at https://client.stadiamaps.com/).
You can also modify it to work with your preferred maps and routing vendor by editing `AppModule.kt`.

Set your API key in `local.properties` to run the demo app:

```properties
stadiaApiKey=YOUR-API-KEY
```
