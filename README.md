# Ferrostar

Ferrostar is a FOSS navigation SDK built from the ground up for the future.

[Why a new SDK?](https://stadiamaps.notion.site/Next-Gen-Navigation-SDK-f16f987bfa5a455296b0671636033cdb)

## Current status

The project is under active development
and the code is not yet ready for use in production yet.
Many parts of the design are still in flux,
so there are no API stability guarantees.

That said, things are now in a solid alpha state,
including a minimum usable UI for iOS.
While there are certainly rough edges, it is indeed usable.
See the getting started section below.

Our next priority is a first pass of the Android UI.

![A screenshot of the current status](screenshot.png)

## Project Goals

- Modular (or one could even say hexagonal) architecture
- Highly extensible (routing backends, UI, etc.)
- Batteries included: Navigation UI should be usable out of the box for the most common use cases in iOS and Android native apps without much configuration
- Vendor-neutrality to allow collaboration among major industry players, hobbyists, and everyone in-between
- No telemetry out of the box. If a use case (ex: fleet management) requires telemetry, this can be added by the developer in their own code.

## Non-goals

- UI components for searching for addresses or building a trip (left to the app developers).
- Compatibility with ancient SDKs / API levels, except where it’s easy; this is a fresh start, so *we can and should leverage modern features and tools* as of 2023.
- Route generation will be handled separately; there are many good cloud vendors (like [Stadia Maps](https://stadiamaps.com/products/navigation-routing/)) as well as self-hosting / local generation options.
- A "free roam" experience without any specific route (though it *should* be possible to plug Ferrostar into such an experience).

## Project Design and Structure

See the [ARCHITECTURE](ARCHITECTURE.md) document.

## Getting Started

### As a Contributor

See our [CONTRIBUTING](CONTRIBUTING.md) guide
for info on expectations and dev environment setup.

### As a User

#### iOS

See the [Demo App](apple/DemoApp) repo
for an up to date demo app that works in your simulator.
The slightly older [ferrostar-ios-demo](https://github.com/stadiamaps/ferrostar-ios-demo)
shows an example that is designed to work with "live" location updates on a real device.
This will soon be completely mereged into the iOS demo app in this repo.

#### Android

Coming soon: We're working on the UI components for Jetpack Compose right now!

## Routing and Basemap Integrations

Ferrostar needs data to be of any use.
In particular, it needs routes to navigate over, and a basemap to be very useful.
We are initially targeting MapLibre and Valhalla for maximum choice and neutrality.

The example applications utilize [MapLibre](https://maplibre.org/) demo tiles for the basemap,
but you'll probably need a more detailed basemap for any sort of real-world use.
We also use the [FOSSGIS e.V. Valhalla server](https://gis-ops.com/global-open-valhalla-server-online/)
as it provides easy access without an API key.
Please respect the fair use policy and limitations of this public service,
and be sure to [find a vendor](VENDORS.md) or run your own service(s) before shipping your app!

## Platform Support Targets

### Rust

The project should always be developed using the latest stable Rust release.
While we don't intend to use every new language features the day it lands,
there isn't any reason to lag behind the latest stable.

### Swift

The core requires Swift 5.9+, as we are iterating on a more ergonomic [wrapper](https://github.com/stadiamaps/maplibre-swiftui-dsl-playground)
for MapLibre Native in parallel,
and it leverages macros.

### iOS

We plan to start iOS support at version 15.
Our general policy will be to support the current and at least the previous major version,
extending to two major versions if possible.

### Android

Android developers should always build using the latest publicly available Android Studio version.
Android users are generally much slower to get new OS major versions due to a variety of factors.

TODO: Determine a reasonable Android support target
