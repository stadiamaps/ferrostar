help:
    @just --list

# Build

# Build the Rust core library. Extra args (e.g. `--verbose`, `--release`) are forwarded to cargo.
[working-directory: 'common']
build-common *args:
    cargo build {{ args }}

# Build the iOS library. Pass `--release` to produce the zipped XCFramework with Package.swift checksum update.
[working-directory: 'common']
build-ios *args:
    ./build-ios.sh {{ args }}

# Fragile-sed territory: if Package.swift's formatting drifts, the substitution silently no-ops.
# Flip Package.swift to local-development mode (useLocalFramework = true).
set-package-swift-local:
    sed -i '' 's/let useLocalFramework = false/let useLocalFramework = true/' Package.swift

# Flip Package.swift back to release mode (useLocalFramework = false).
set-package-swift-release:
    sed -i '' 's/let useLocalFramework = true/let useLocalFramework = false/' Package.swift

# Build the Android library
[working-directory: 'android']
build-android:
    ./gradlew build

# Build the web library
[working-directory: 'web']
build-web:
    npm install
    npm run build

# Test

ios-sim-destination := 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2'

# Run the full common library (Rust) test suite
[working-directory: 'common']
test-common:
    cargo nextest run --no-fail-fast
    cargo test --doc

# Check MSRV (matches CI). Requires `cargo install cargo-hack`.
[working-directory: 'common']
check-msrv:
    cargo hack check --rust-version --workspace --all-targets --ignore-private

# Check for semver violations that would require a version bump
[working-directory: 'common']
check-semver:
    cargo semver-checks check-release --default-features

# Run the full iOS test suite. Pipes through xcbeautify when available.
# Pass `--ci` to also emit FerrostarCore-Package.xcresult (large bundle; CI uploads it as an artifact).
[script('bash')]
[arg("ci", long="ci", value="true")]
test-ios ci="false": build-ios
    if command -v xcbeautify >/dev/null 2>&1; then
        xcodebuild -scheme FerrostarCore-Package test -skipMacroValidation -destination '{{ ios-sim-destination }}' {{ if ci == "true" { "-resultBundlePath FerrostarCore-Package.xcresult" } else { "" } }} | xcbeautify
        exit ${PIPESTATUS[0]}
    else
        echo "xcbeautify not installed; falling back to plain xcodebuild output" >&2
        xcodebuild -scheme FerrostarCore-Package test -skipMacroValidation -destination '{{ ios-sim-destination }}' {{ if ci == "true" { "-resultBundlePath FerrostarCore-Package.xcresult" } else { "" } }}
    fi

# Run full Android tests
[working-directory: 'android']
test-android: test-android-unit test-android-snapshots test-android-connected

# Run the Android unit test suite (narrow scope; most tests are in connected checks)
[working-directory: 'android']
test-android-unit:
    ./gradlew test

# Run Android connected checks (requires the user start an emulator or attach a device first!)
[working-directory: 'android']
test-android-connected:
    ./gradlew connectedCheck

# Run Android UI snapshot tests
[working-directory: 'android']
test-android-snapshots:
    ./gradlew verifyPaparazziDebug

# Update Android UI snapshots
[working-directory: 'android']
record-android-snapshots:
    ./gradlew recordPaparazziDebug

# Run the web test suite
[working-directory: 'web']
test-web:
    npm install && npm test

# Run

# Install and launch the Android demo app on a connected device.
# With one device connected, it's used automatically.
# With multiple devices, an interactive menu lets you pick one.
# Device targeting uses the ANDROID_SERIAL env var (no hard-coded IDs).
[working-directory: 'android']
[script('bash')]
run-android-demo:
    # Locate adb
    if [ -n "$ANDROID_HOME" ]; then
        ADB="$ANDROID_HOME/platform-tools/adb"
    elif [ -f local.properties ]; then
        SDK_DIR=$(grep '^sdk.dir=' local.properties | cut -d'=' -f2)
        ADB="$SDK_DIR/platform-tools/adb"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        ADB="$HOME/Library/Android/sdk/platform-tools/adb"
    elif [ -d "$HOME/Android/Sdk" ]; then
        ADB="$HOME/Android/Sdk/platform-tools/adb"
    else
        echo "Error: Cannot find Android SDK. Set ANDROID_HOME or add sdk.dir to local.properties."
        exit 1
    fi

    if [ ! -x "$ADB" ]; then
        echo "Error: adb not found at $ADB"
        exit 1
    fi

    # Detect connected devices
    DEVICES=$("$ADB" devices | tail -n +2 | grep -w 'device' | cut -f1)
    DEVICE_COUNT=$(echo "$DEVICES" | grep -c . || true)

    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "Error: No connected devices found. Connect a device or start an emulator."
        exit 1
    elif [ "$DEVICE_COUNT" -eq 1 ]; then
        SERIAL="$DEVICES"
        echo "Using device: $SERIAL"
    else
        echo "Multiple devices connected. Select one:"
        select SERIAL in $DEVICES; do
            if [ -n "$SERIAL" ]; then
                break
            fi
            echo "Invalid selection. Try again."
        done
    fi

    export ANDROID_SERIAL="$SERIAL"

    ./gradlew :demo-app:installDebug
    "$ADB" shell am start -n com.stadiamaps.ferrostar.demo/com.stadiamaps.ferrostar.MainActivity

# Run a local development web build
[working-directory: 'web']
run-web-dev:
    npm run dev

# Formatting
#
# Every `format-*` recipe accepts a `--check` flag. When passed, the recipe runs the
# validation-only variant (e.g. `cargo fmt --check`) instead of writing. CI uses `--check`.

# Run all code formatters (writes changes by default; `--check` just checks for violations)
[arg("check", long="check", value="true")]
format-all check="false": (format-apple check) (format-android check) (format-web check) (format-common check)

# Format Apple code
[arg("check", long="check", value="true")]
format-apple check="false":
    swiftformat . {{ if check == "true" { "--lint" } else { "" } }}

# Format Android code
[working-directory: 'android']
[arg("check", long="check", value="true")]
format-android check="false":
    ./gradlew {{ if check == "true" { "ktfmtCheck" } else { "ktfmtFormat" } }}

# Format web code
[working-directory: 'web']
[arg("check", long="check", value="true")]
format-web check="false":
    npm install  # JS formatting tools are bespoke per project/version so they have to be installed.
    npm run {{ if check == "true" { "format:check" } else { "format:fix" } }}
    npm run {{ if check == "true" { "lint" } else { "lint:fix" } }}

# Format all Rust code
[working-directory: 'common']
[arg("check", long="check", value="true")]
format-common check="false":
    cargo fmt {{ if check == "true" { "--check" } else { "" } }}

# Docs

# Live-preview the guide locally (mdbook serve).
[working-directory: 'guide']
preview-guide:
    mdbook serve

# Clean all targets
clean:
    cd common && cargo clean
    cd android && ./gradlew clean
    # JS cleanup; `-prune` stops the walk at each match so nested node_modules aren't re-traversed.
    find . -name node_modules -type d -prune -exec rm -rf {} +
