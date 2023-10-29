We're stoked that you're interested in working on Ferrostar!
This contribution guide will get you started developing in no time,
as well as provide some guidelines to follow when submitting an issue or PR.

## Best Practices for Contributions

We welcome contributions from community!
Due to the size and complexity of the code, below are some best practices that ensure smooth collaboration.

It is a good idea to discuss large proposed changes before proceeding to an issue ticket or PR.
The project team is active in the following forums:

* For informal chat discussions, visit the `#ferrostar` channel in the OSMUS Slack.
  You can get an invite to the workspace at [slack.openstreetmap.us](https://slack.openstreetmap.us/).
* For larger discussions where it would be desirable to have wider input / a less ephemeral record,
  consider starting a thread on [GitHub Discussions](https://github.com/stadiamaps/ferrostar/discussions).
  This makes it easier to find and reference the discussion in the future. 

Both new features and bugfixes should update or add unit test cases where possible
to prevent regressions and demonstrate correctness.
This is particularly true of the common core.
We are a bit more lax with the frontend code as this may be difficult or impractical to test.
(TODO: Look into snapshot testing as a way to overcome this issue.
There is a great [iOS framework](https://github.com/pointfreeco/swift-snapshot-testing) available.
Unsure of the state of Android.)

### New Features

For new features, you should generally start by opening a new issue.
That will allow for separate tracking of discussion of the feature itself
and (if you're proposing code as well) the implementation of the feature.

### Bug Fixes

If you've identified a significant bug, or one that you don't intend to fix yourself,
please write up an issue ticket describing the problem.
For minor or straightforward bug fixes, feel free to proceed directly to a PR.

## Preparing your Development Environment

### Rust

The common core is written in Rust.
If you don't already have Rust, you can grab it following the instructions [here](https://www.rust-lang.org/).
If at all possible, install `rustup`.
We use [rust-toolchain.yml](common/rust-toolchain.yml) to synchronize the toolchain
and install targets automatically.

### iOS

* Install the latest version of Xcode
* Install the Xcode Command Line Tools

```shell
xcode-select --install
```

* Since you're developing locally, set `let useLocalFramework = true` in `Package.swift`. 
* Run the iOS build script

```shell
cd ./common
./build-ios.sh
```

**IMPORTANT:** every time you make changes to the common core,
you will need to run [`build-ios.sh`](common/build-ios.sh) to see your changes on iOS!
It would be great to integrate this into the Xcode build flow in the future,
but at the time of this writing,
it is not possible with the Swift package flow.
Further, the "normal" Xcode build flow always assumes xcframeworks can't change during build,
so it processes them before any other build rules.
Given these limitations, we opted for a shell script until further notice.

### Android

NOTE: Android is probably broken at the moment!
This should be fixed in November.
If not, bug Ian.

* Install [Android Studio](https://developer.android.com/studio).
* Ensure that the latest NDK is installed
  (refer to the version number in [`build-android.sh`](core/build-android.sh)
  and ensure you have the same version available;
  we try to track the latest stable).

Additionally, you will need to add something like this to your Cargo config (usually `~/.cargo/config.toml`).
This is necessary to be able to link correctly,
and will be machine-dependent based on the NDK you have installed and exact paths.
**NOTE**: You *must* use absolute paths.
Refer to the version number in [`build-android.sh`](core/build-android.sh)
to ensure you have the correct API level available.

```toml
[target.aarch64-linux-android]
linker = "/Users/ianthetechie/Library/Android/sdk/ndk/25.2.9519653/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android33-clang"

[target.armv7-linux-androideabi]
linker = "/Users/ianthetechie/Library/Android/sdk/ndk/25.2.9519653/toolchains/llvm/prebuilt/darwin-x86_64/bin/armv7a-linux-androideabi33-clang"

[target.i686-linux-android]
linker = "/Users/ianthetechie/Library/Android/sdk/ndk/25.2.9519653/toolchains/llvm/prebuilt/darwin-x86_64/bin/i686-linux-android33-clang"

[target.x86_64-linux-android]
linker = "/Users/ianthetechie/Library/Android/sdk/ndk/25.2.9519653/toolchains/llvm/prebuilt/darwin-x86_64/bin/x86_64-linux-android33-clang"
```

After the initial setup, Gradle should be able to handle rebuilding the core for Android as-needed.

## Writing & Running Tests

### Common Core

Run `cargo test -p ferrostar-core` from within the `common` directory to run tests.

### iOS

Run unit tests as usual from within Xcode.

### Android

Run `gradle test` *as well as* the Android Tests within Android Studio.
(TODO: CLI info)

## Code Conventions

* Format all Rust code using `cargo fmt`
* Run `cargo clippy` and either fix any warnings or document clearly why you think the linter should be ignored
* All iOS code must be written in Swift
* TODO: Swiftlint and swift-format?
* All Android code must be written in Kotlin
* TODO: Android linter + formatter?

## Changelog Conventions

NOTE: We'll be *extremely* loose with this till we get closer to a minimally usable state.

What warrants a changelog entry?

- Any change that affects the public API, visual appearance or user security *must* have a changelog entry
- Any performance improvement or bugfix *should* have a changelog entry
- Any contribution from a community member *may* have a changelog entry, no matter how small
- Any documentation related changes *should not* have a changelog entry
- Any regression change introduced and fixed within the same release *should not* have a changelog entry
- Any internal refactoring, technical debt reduction, render test, unit test or benchmark related change *should not* have a changelog entry

How to add your changelog?

- Edit the [`CHANGELOG.md`](CHANGELOG.md) file directly, inserting a new entry at the top of the appropriate list
- Any changelog entry should be descriptive and concise; it should explain the change to a reader without context

