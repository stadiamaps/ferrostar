#!/bin/bash

bun run --elide-lines=0 --filter @stadiamaps/ferrostar-uniffi-react-native ubrn:clean

bun run --elide-lines=0 --filter @stadiamaps/ferrostar-uniffi-react-native ubrn:android

bun run --elide-lines=0 --filter @stadiamaps/ferrostar-uniffi-react-native build

bun run --elide-lines=0 --filter @stadiamaps/ferrostar-uniffi-react-native codegen

bun run --elide-lines=0 --filter @stadiamaps/ferrostar-core-react-native build

bun run --elide-lines=0 --filter @stadiamaps/ferrostar-maplibre-react-native build

bun run --elide-lines=0 --filter @stadiamaps/ferrostar-example-react-native prebuild
