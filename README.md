# Ferrostar

Ferrostar is a FOSS navigation SDK built from the ground up for the future.

[Why a new SDK?](https://stadiamaps.notion.site/Next-Gen-Navigation-SDK-f16f987bfa5a455296b0671636033cdb)

## Project Goals

- Modular (or one could even say hexagonal) architecture
- Highly extensible (routing backends, UI, etc.)
- Batteries included: Navigation UI should be usable out of the box for the most common use cases in iOS and Android native apps without much configuration
- Vendor-neutrality to allow collaboration among major industry players, hobbyists, and everyone in-between
- No telemetry out of the box. If a use case (ex: fleet management) requires telemetry, this can be added by the developer in their own code.

## Non-goals

- UI components for searching for addresses or building a trip (left to the app developers).
- Compatibility with ancient SDKs / API levels, except where it’s easy; this is a fresh start, so *we can and should leverage modern features and tools* as of 2023.
- Route generation is a separate concern; there are many good cloud vendors (like [Stadia Maps](https://stadiamaps.com/products/navigation-routing/)) as well as self-hosting / local generation options. Further, we assume that the external route generator is responsible for things like text and voice prompt generation (ex: “In 500 feet, take exit 12”).

## Project Design and Structure

See the [ARCHITECTURE](ARCHITECTURE.md) document.

## Getting Started

### As a Contributor

See our [CONTRIBUTING](CONTRIBUTING.md) guide for info on expectations and dev environment setup.

### As a User

TODO: Write this once we have something useful.

#### TODO: iOS

#### TODO: Android

## Platform Support Targets

### Rust

The project should always be developed using the latest stable Rust release. While we don't necessarily
*intend* to use new language features the day they land, there really isn't any reason to lag behind the latest
stable.

### iOS

iOS developers should always build using the latest publicly available Xcode version. As far as OS support goes,
we will initially target the current iOS major version (16.0). It is already run by 72% of devices, and iOS 16
introduced many helpful changes. As new releases appear, we will support either one or two previous major versions
(depending on the desirability of new features and adoption rate). This will minimize the amount of cruft build-up.
iOS users are fairly quick to upgrade compared to other platforms, so supporting the latest 2 major versions
typically results in 80% (shortly after release) to 90% (after around 6 months) device support.

### Android

Android developers should always build using the latest publicly available Android Studio version.
Android users are generally much slower to get new OS major versions due to a variety of factors, so
our Android support will initially stretch back to API level 29 (Android 10 / Q). This will similarly cover around 70%
of devices to start.

TODO: Long term, what is a reasonable Android support target? 90%?
