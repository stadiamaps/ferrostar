# Ferrostar Common

This is the common core of Ferrostar, which is shared across all platforms. The common parts of navigation
logic are kept internal wherever possible, with a public interface exposed to platform-specific code
via FFI bindings. Bindings are generated using [UniFFI](https://mozilla.github.io/uniffi-rs/).

## `ferrostar`

This is the main crate. At a high level, this defines two critical pieces: the navigation controller
and routing backends, which are described in detail in [ARCHITECTURE ](../ARCHITECTURE.md).

## `uniffi-bindgen`

This crate provides a binary target, `uniffi-bindgen`, which generates Kotlin and Swift bindings.
You probably don't need to touch this crate. It's just here because no canonical binary exists on crates.io.

## Getting Started

### Android

Install cargo-ndk to allow gradle to build the local library `libferrostar.so` and `libuniffi_ferrostar.so`. With cargo-ndk installed you can load and sync Android Studio then build the demo app allowing gradle to automatically build what it needs.

```sh
cargo install cargo-ndk
```

### iOS 

Run the build iOS shell command to build the xcframework required to locally run the Swift Package and demo app.

```sh
cd common
./build-ios.sh
```