#!/bin/zsh

set -e
set -u

# NOTE: You MUST run this every time you make changes to the core. Unfortunately, calling this from Xcode directly
# does not work so well.

# Potential optimizations for the future:
#
# * Only build one simulator arch for local development (we build both since many still use Intel Macs)
# * Option to do debug builds instead for local development
fat_simulator_lib_dir="target/ios-simulator-fat/release"

generate_ffi() {
  echo "Generating framework module mapping and FFI bindings"
  cargo run -p uniffi-bindgen generate $1/$2.udl --language swift --out-dir target/uniffi-xcframework-staging-$2
  mv target/uniffi-xcframework-staging-$2/*.swift ../SwiftCore/Sources/FFI/
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
}

cargo build --lib --release --target x86_64-apple-ios
cargo build --lib --release --target aarch64-apple-ios-sim
cargo build --lib --release --target aarch64-apple-ios

generate_ffi ferrostar-core/src ferrostar
create_fat_simulator_lib libferrostar_core
build_xcframework libferrostar_core ferrostar
