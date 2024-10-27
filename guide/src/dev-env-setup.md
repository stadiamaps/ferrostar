# Developer Environment Setup

To ensure that everything can be developed properly in parallel,
we use a monorepo structure.
This, combined with CI, will ensure that changes in the core must be immediately reflected in platform code
like Apple and Android.

Let's look at what's involved to get hacking on each platform.

### Rust

1. Install [Rust](https://www.rust-lang.org/).
   If at all possible, install `rustup`.
   We use [rust-toolchain.yml](common/rust-toolchain.yml)
   to synchronize the toolchain and install targets automatically
   (otherwise you will need to manage toolchains manually).
2. Open the cargo workspace (`common/`) in your preferred editing environment. 

The Rust project is a cargo workspace,
and nothing beyond the above should be needed to start hacking!
Make some changes and run the tests!

#### PR checklist

Before pushing, run the following from the `common` folder:

* Run `cargo fmt` to automatically format any new rust code.
* Bump the version on the ferrostar Cargo.toml at `common/ferrostar/Cargo.toml` (if necessary).
  If you forget to do this and make breaking changes, CI will let you know.

### Web

Perform all commands unless otherwise noted from the `web` directory.

1. Install `wasm-pack`:

```shell
cargo install wasm-pack
```

2. Build the WASM package for the core:

```shell
npm run prepare:core
```

3. Install dependencies:

```shell
npm install
```

4. Run a local dev server or do a release build:

```shell
# This will start a local web server for the demo app with hot reload
npm run dev

# Or you can do a release build (we test this in CI)
npm run build
```

#### PR checklist

Run `npm run format:fix` from the `web` directory before committing
to ensure consistent formatting.

### iOS

1. Install the latest version of Xcode.
2. Install the Xcode Command Line Tools.

```shell
xcode-select --install
```

3. Install [`swiftformat`](https://github.com/nicklockwood/SwiftFormat).
4. Since you're developing locally, set `let useLocalFramework = true` in `Package.swift`.
   (TODO: Figure out a way to extract this so it doesn't get accidentally committed.) 
5. Run the iOS build script:

```shell
cd common
./build-ios.sh
```

**IMPORTANT:** every time you make changes to the common core,
you will need to run [`build-ios.sh`](common/build-ios.sh) to see your changes on iOS!
We want to integrate this into the Xcode build flow in the future,
but at the time of this writing,
it is not possible with the Swift package flow.
Further, the "normal" Xcode build flow always assumes `xcframeworks` can't change during build,
so it processes them before any other build rules.
Given these limitations, we opted for a shell script until further notice.

5. Open the Swift package in Xcode.
   (NOTE: Due to the quirks of how SPM is designed,
   Package.swift must live in the repo root.
   This makes the project view in Xcode slightly more cluttered,
   but there isn't much we can do about this given how SPM works.)

#### PR checklist

Run `swiftformat .` from the `apple` directory before committing
to ensure consistent formatting.

### Android

1. Install [Android Studio](https://developer.android.com/studio) (NOTE: We assume you are using a recent version no more than ~a month out of date).
2. Install cargo-ndk to allow gradle to build the local library `libferrostar.so` and `libuniffi_ferrostar.so`. 
   With cargo-ndk installed you can load and sync Android Studio then build the demo app allowing gradle to 
   automatically build what it needs.

```sh
cargo install cargo-ndk
```

3. Ensure that the latest NDK is installed
   (refer to the `ndkVersion` number in [`core/build.gradle`](android/core/build.gradle)
   and ensure you have the same version available).
   This is easiest to install via Android Studio's SDK Manager (under SDK Tools > NDK).
4. Open the Gradle workspace ('android/') in Android Studio.
   Gradle builds automatically ensure the core is built,
   so there are no funky scripts needed as on iOS.
5. (Optional) If you want to use Maven local publishing to test...
   - Bump the version number to a `SNAPSHOT` in `build.gradle`.
   - run `./gradlew publishToMavenLocal -Pskip.signing`.
   - Reference the updated version number in the project, and ensure that `mavenLocal` is one of the `repositories`.

#### PR checklist

Run the `ktfmtFormat` gradle action before committing to ensure consistent formatting.
