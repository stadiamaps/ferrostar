# Ferrostar Android

This directory tree contains the Gradle workspace for Ferrostar on Android.

* `composeui` - Jetpack Compose UI elements which are not tightly coupled to any particular map renderer.
* `core` - The core module is where all the "business logic", location management, and other core functionality lives.
* `demo-app` - A minimal demonstration app.
* `google-play-services` - Optional functionality that depends on Google Play Services (like the a fused location client wrapper). This is a separate module so that apps are able to "de-Google" if necessary.
* `maplibreui` - Map-related user interface components built with MapLibre.