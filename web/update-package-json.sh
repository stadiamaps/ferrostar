#!/usr/bin/env zsh

set -e
set -u

# Get the version of the crate (the wasm package version always matches this)
version=$(cd ../common && cargo metadata --format-version 1 | jq -r '.packages[] | select(.name=="ferrostar") .version')

# Replace the local file reference with a public version
sed -i "" -E "s/file\:\.\.\/common\/ferrostar\/pkg/\^$version/g" package.json
