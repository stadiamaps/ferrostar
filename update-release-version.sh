#!/usr/bin/env zsh

set -e
set -u

version=$1
sed -i "" -E "s/(let releaseTag = \")[^\"]+(\")/\1$version\2/g" Package.swift
sed -i "" -E "s/(version = \")[^\"]+(\")/\1$version\2/g" android/build.gradle
awk '{ if (!done && /version = \"/) { sub(/(version = \")[^\"]+(\")/, "version = \"" newVersion "\""); done=1 } print }' newVersion="$version" common/ferrostar/Cargo.toml > tmpfile && mv tmpfile common/ferrostar/Cargo.toml
cd common && cargo check && cd ..

git add Package.swift android/build.gradle common/Cargo.lock common/ferrostar/Cargo.toml
