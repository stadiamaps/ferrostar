# ``FerrostarCore``

A modern, open-source navigation SDK for iOS and watchOS, built on a shared Rust core.

## Overview

FerrostarCore provides the fundamental building blocks for turn-by-turn navigation on Apple platforms. It handles location tracking, route parsing, state machine transitions, and instructions, allowing you to build highly customized navigation experiences or use our pre-built UI components.

### Core Concepts

To get started with Ferrostar, you'll primarily interact with these core subsystems:

- **NavigationController**: The central state machine managing the active navigation session.
- **RouteProvider**: Integrates with routing engines (like Valhalla) to fetch directions.
- **LocationProvider**: Feeds location updates to the state machine.

## Topics

### Core Architecture
- ``FerrostarCore``

### Getting Started
For an end-to-end integration guide, check out our official [User Guide](https://stadiamaps.github.io/ferrostar/).