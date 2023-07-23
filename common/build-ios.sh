#!/bin/zsh

set -e
set -u

# NOTE: You MUST run this every time you make changes to the core. Unfortunately, calling this from Xcode directly
# does not work so well.

# In release mode, we create a ZIP archive of the xcframework and update Package.swift with the computed checksum.
# This is only needed when cutting a new release, not for local development.
release=false

for arg in "$@"
do
    case $arg in
        --release)
            release=true
            shift # Remove --release from processing
            ;;
        *)
            shift # Ignore other argument from processing
            ;;
    esac
done


# Potential optimizations for the future:
#
# * Only build one simulator arch for local development (we build both since many still use Intel Macs)
# * Option to do debug builds instead for local development
fat_simulator_lib_dir="target/ios-simulator-fat/release"

generate_ffi() {
  echo "Generating framework module mapping and FFI bindings"
  cargo run -p uniffi-bindgen generate $1/$2.udl --language swift --out-dir target/uniffi-xcframework-staging-$2
  mv target/uniffi-xcframework-staging-$2/*.swift ../apple/Sources/UniFFI/
  mv target/uniffi-xcframework-staging-$2/$2FFI.modulemap target/uniffi-xcframework-staging-$2/module.modulemap  # Convention requires this have a specific name
}

create_fat_simulator_lib() {
  echo "Creating a fat library for x86_64 and aarch64 simulators"
  mkdir -p $fat_simulator_lib_dir
  lipo -create target/x86_64-apple-ios/release/$1.a target/aarch64-apple-ios-sim/release/$1.a -output $fat_simulator_lib_dir/$1.a
}

build_xcframework() {
  # Builds an XCFramework
  echo "Generating XCFramework"
  rm -rf target/ios  # Delete the output folder so we can regenerate it
  xcodebuild -create-xcframework \
    -library target/aarch64-apple-ios/release/$1.a -headers target/uniffi-xcframework-staging-$2 \
    -library target/ios-simulator-fat/release/$1.a -headers target/uniffi-xcframework-staging-$2 \
    -output target/ios/$2-rs.xcframework

  if $release; then
    echo "Building xcframework archive"
    zip -r target/ios/$2-rs.xcframework.zip target/ios/$2-rs.xcframework
    checksum=$(swift package compute-checksum target/ios/$2-rs.xcframework.zip)
    version=$(cargo metadata --format-version 1 | jq -r '.packages[] | select(.name=="ferrostar-core") .version')
    sed -i "" -E "s/(let releaseTag = \")[^\"]+(\")/\1$version\2/g" ../Package.swift
    sed -i "" -E "s/(let releaseChecksum = \")[^\"]+(\")/\1$checksum\2/g" ../Package.swift
  fi
}

cargo build --lib --release --target x86_64-apple-ios
cargo build --lib --release --target aarch64-apple-ios-sim
cargo build --lib --release --target aarch64-apple-ios

generate_ffi ferrostar-core/src ferrostar
create_fat_simulator_lib libferrostar_core
build_xcframework libferrostar_core ferrostar
