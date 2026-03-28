#!/bin/bash

bun run --filter @stadiamaps/ferrostar-uniffi-react-native ubrn:clean

bun run --filter @stadiamaps/ferrostar-uniffi-react-native ubrn:android

bun run --filter @stadiamaps/ferrostar-uniffi-react-native prepare

bun run --filter @stadiamaps/ferrostar-uniffi-react-native codegen

bun run --filter @stadiamaps/ferrostar-core-react-native prepare

bun run --filter @stadiamaps/ferrostar-maplibre-react-native prepare

bun run --filter @stadiamaps/ferrostar-example-react-native prebuild
