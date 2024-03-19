# Platform Support Targets

## Rust

The core team develops using the latest stable Rust release.
At the moment, or MSRV is generally dictated by downstream projects.
Until the project is stable, it is unlikely that we will have a more formal MSRV policy,
but we do document the current MSRV in the root `Cargo.toml` and verify it via CI.

## Swift

The core requires Swift 5.9+.
We are iterating on a more ergonomic [wrapper](https://github.com/stadiamaps/maplibre-swiftui-dsl-playground) for MapLibre Native on iOS,
and this leverages macros, which drive this requirement.

## iOS

We plan to start iOS support at version 15.
Our general policy will be to support the current and at least the previous major version,
extending to two major versions **if possible**.

## Android

Android developers should always build using the latest publicly available Android Studio version.
Android users are generally much slower to get new OS major versions due to a variety of factors,
which will influence our eventual decision on a minimum Android API level.
We will publish a more formal support policy closer to 1.0

