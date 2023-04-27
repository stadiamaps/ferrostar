#!/bin/zsh

set -e
set -u

cargo build --lib --release --target aarch64-linux-android
cargo build --lib --release --target armv7-linux-androideabi
cargo build --lib --release --target i686-linux-android
cargo build --lib --release --target x86_64-linux-android
