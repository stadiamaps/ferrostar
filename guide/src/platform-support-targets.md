# Platform Support Targets

We’re building a navigation SDK for the future,
but we acknowledge that your users live in the present.
Our general policy is to expect developers to have up-to-date build tools,
but support older devices *where possible*
without compromising the maintainability and future-readiness of the project.

## Rust

The core team develops using the latest stable Rust release.
At the moment, our MSRV is dictated by downstream projects.
Until the project is stable, it is unlikely that we will have a more formal MSRV policy,
but we do document the current MSRV in the root `Cargo.toml` and verify it via CI.

## Swift

The core requires Swift 5.9+.
We are iterating on a more ergonomic [wrapper](https://github.com/stadiamaps/maplibre-swiftui-dsl-playground) for MapLibre Native on iOS,
and this leverages macros, which drive this requirement.

## iOS

iOS support starts at version 15.
Our general policy will be to support the current and at least the previous major version,
extending to two major versions **if possible**.
At the time of this writing, the “current-1” policy covers 96% of iOS devices.
This is a pretty standard adoption rate for iOS.

## Android

Android developers should always build using the latest publicly available Android Studio version.
Android users are much slower to get new OS major versions due to a variety of factors.

We currently support API level 25 and higher (with some caveats).
At the time of this writing, it covers 96% of Android users.
We will use publicly available data on API levels and developer feedback
to set API level requirements going forward.

### Android API Level caveats

API levels lower than 26 do not include support for several Java 8 APIs.
Crucially, the `Instant` API, which is essential for the library, is not present.
If you cannot raise your minimum SDK to 26 or higher,
you may need to enable [Java 8+ API desugaring support](https://developer.android.com/studio/write/java8-support).

Also note that Android before API 30 has to fall back on some older ICU APIs.
We recommend supporting the newest API version possible for your user base,
as Google officially drops support for older releases after just a few years.