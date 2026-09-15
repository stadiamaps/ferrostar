#!/bin/bash

bun run --filter @stadiamaps/ferrostar-uniffi-react-native ubrn:clean

bun run --filter @stadiamaps/ferrostar-uniffi-react-native clean

bun run --filter @stadiamaps/ferrostar-core-react-native clean

bun run --filter @stadiamaps/ferrostar-maplibre-react-native clean
