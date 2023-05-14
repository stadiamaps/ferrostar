We're stoked that you're interested in working on Ferrostar! This contribution guide will get you started developing in no time, as well
as provide some guidelines to follow when submitting an issue or PR.

## Best Practices for Contributions

We welcome contributions from community! Due to the size and complexity of the code, below are some best practices that ensure a smooth
collaboration.

It is a good idea to discuss large proposed changes before proceeding to an issue ticket or PR. The project team is active in the following forums:

* For informal chat discussions, visit the `#ferrostar` channel in the OSMUS Slack. You can get an invite to the workspace at [slack.openstreetmap.us](https://slack.openstreetmap.us/).
* For discussions whose output and outcomes should not be ephemeral, consider starting a thread on [GitHub Discussions](https://github.com/stadiamaps/ferrostar/discussions). This makes it easier to find and reference the discussion in the future. 

Both new features and bugfixes should update or add unit test cases where possible to prevent regressions and demonstrate correctness.
This is particularly true of the common core. We are a bit more lax with the frontend code as this is often difficult or impractical to test.

### New Features

For new features, you should generally start by opening a new issue. That will allow for separate tracking of discussion of the
feature itself and (if you're proposing code as well) the implementation of the feature.

### Bug Fixes

If you've identified a significant bug, or one that you don't intend to fix yourself, please write up an issue ticket describing the problem. For minor or straightforward bug fixes, feel free to proceed directly to a PR.

Some best practices for PRs for bugfixes are as follows:

1. Begin by writing a failing test which demonstrates how the current software fails to operate as expected. Commit and push the branch.
2. Create a draft PR which documents the incorrect behavior. This will show the failing test you've just written in the project's continuous integration and demonstrates the existence of the bug.
3. Fix the bug, and update the PR with any other notes needed to describe the change in the PR's description.
4. Don't forget to mark the PR as ready for review when you're satisfied with the code changes.

This is not intended to be a strict process but rather a guideline that will build confidence that your PR is addressing the problem.

## Preparing your Development Environment

### Rust

The common core is written in Rust. If you don't already have this, you can grab it [here](https://www.rust-lang.org/).
If at all possible, install `rustup` as we use [rust-toolchain.yml](common/rust-toolchain.yml) to synchronize the toolchain and install
targets automatically.

### iOS

* Install the latest version of Xcode (from the App Store is usually easiest)

```bash
# Install the Xcode Command Line Tools
xcode-select --install
```

**IMPORTANT:** every time you make changes to the common core, you will need to run [`build-ios.sh`](common/build-ios.sh)! It would be
great to integrate this into the Xcode build flow in the future, but at the time of this writing, it is not possible with the Swift package
flow. Further, the "normal" Xcode build flow always assumes xcframeworks can't change during build, so it processes them before any other
build rules. So we use a shell script.

If Xcode is not behaving well (it often caches Swift packages too), a quick restart of Xcode or resetting the package graph usually
clears it up.

TODO: Decide on how to publish the Swift package. Probably something convoluted like MapLibre does, since SPM is so picky about owning the
repo root.

### Android

* Install [Android Studio](https://developer.android.com/studio).
* Ensure that the latest NDK is installed (refer to the version number in [`build-android.sh`](core/build-android.sh) and ensure you have the same version available; we try to track the latest stable).

Additionally, you will need to add something like this to your Cargo config (usually `~/.cargo/config.toml`).
This is necessary to be able to link correctly, and will be machine-dependent based on the NDK you have installed and exact paths.
**NOTE**: You *must* use absolute paths. Refer to the version number in [`build-android.sh`](core/build-android.sh) to ensure you have
the correct API level available.

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

* Run `cargo test -p ferrostar-core` from within the `common` directory to run tests.
* We should strive to keep the core well-tested, using unit tests and/or integration tests as appropriate. Please write tests before submitting most PRs.

TODO:
* For iOS, run unit tests as usual from within Xcode.
* `gradle test` or something for Android.

## Code Conventions

* Format all Rust code using `cargo fmt`
* Run `cargo clippy` and either fix any warnings or document clearly why you think the linter should be ignored (it's usually correct)
* All iOS code must be written in Swift
* TODO: Swiftlint and swift-format
* All Android code must be written in Kotlin
* TODO: Android linter + formatter?

### Version Control Conventions

Here is a recommended way to get setup:
1. Fork this project
2. Clone your new fork
4. Add the MapLibre repository as an upstream repository: `git remote add upstream git@github.com:stadiamaps/ferrostar.git`
5. Create a new branch `git checkout -b your-branch` for your contribution
6. Write code, open a PR from your branch when you're ready
7. If you need to rebase your fork's PR branch onto main to resolve conflicts: `git fetch upstream`, `git rebase upstream/main` and force push to Github `git push --force origin your-branch`

## Changelog Conventions

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

