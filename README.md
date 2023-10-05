# Ferrostar

Ferrostar is a FOSS navigation SDK built from the ground up for the future.

[Why a new SDK?](https://stadiamaps.notion.site/Next-Gen-Navigation-SDK-f16f987bfa5a455296b0671636033cdb)

## Current status

The project is under active development and the code is not yet ready for use in apps yet.
We are still working out multiple elements of the high-level design and will post updates as interfaces and other
pieces of the design start to stabilize. You can track the road to something _usable_ in the
[Proof of Concept Milestone](https://github.com/stadiamaps/ferrostar/milestone/1).

That said, we are coming quickly to a point where we can actually iterate on concepts, and we are already able
to run end-to-end unit tests that call Rust code from an idiomatic wrapper on iOS. We are focusing on
iterating on the design with iOS first, and once we feel we have answered the important design questions,
we can translate the concepts to Android and iterate on the core in parallel.

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

See our [CONTRIBUTING](CONTRIBUTING.md) guide for info on expectations and dev environment setup.

NOTE: The Android project will probably be broken for a few weeks, and you'll definitely want to be building with
`useLocalFramework = true` in Package.swift.

### As a User

TODO: Write this once we have something useful.

#### TODO: iOS

#### TODO: Android

## Platform Support Targets

### Rust

The project should always be developed using the latest stable Rust release. While we don't necessarily
*intend* to use new language features the day they land, there really isn't any reason to lag behind the latest
stable.

### Swift

Our initial Swift compiler requirement will be set at 5.5, since we will be leveraging async/await.

### iOS

We will initially target the current iOS major version (16.0). It is already run by 72% of devices, and iOS 16 introduced many helpful changes.
As new releases appear, we will eventually make some decisions on how far back to support (depending on the desirability
of new features and adoption rate). Fortunately iOS users are fairy quick to upgrade.

### Android

Android developers should always build using the latest publicly available Android Studio version.
Android users are generally much slower to get new OS major versions due to a variety of factors, so
our Android support will initially stretch back to API level 29 (Android 10 / Q). This will similarly cover around 70%
of devices to start.

TODO: Long term, what is a reasonable Android support target? 90%?
