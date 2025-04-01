#!/bin/bash

yarn workspace @stadiamaps/ferrostar-uniffi-react-native ubrn:clean

yarn workspace @stadiamaps/ferrostar-uniffi-react-native ubrn:android

yarn workspace @stadiamaps/ferrostar-uniffi-react-native prepare

yarn workspace @stadiamaps/ferrostar-uniffi-react-native codegen

yarn workspace @stadiamaps/ferrostar-core-react-native prepare

yarn workspace @stadiamaps/ferrostar-maplibre-react-native prepare
