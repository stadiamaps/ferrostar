# @stadiamaps/ferrostar-core-react-native

Core navigation functionality for React Native applications using the Ferrostar navigation library.

## Features

- Turn-by-turn navigation core functionality
- Real-time location tracking and route following
- Route deviation detection and automatic rerouting
- Support for alternative routes
- Custom location providers
- TypeScript support
- Comprehensive navigation state management

## Installation

```bash
npm install @stadiamaps/ferrostar-core-react-native
# or
yarn add @stadiamaps/ferrostar-core-react-native
```

## Requirements

### Peer Dependencies
- React Native
- React
- @react-native-community/geolocation (^3.4.0)

## Basic Usage

```typescript
import { FerrostarCore, NavigationControllerConfig } from '@stadiamaps/ferrostar-core-react-native';

// Initialize the core
const core = new FerrostarCore(
  'YOUR_VALHALLA_ENDPOINT_URL',
  'auto',
  new NavigationControllerConfig({
    // configuration options
  })
);

// Get routes
const routes = await core.getRoutes(userLocation, waypoints);

// Start navigation with selected route
core.startNavigation(routes[0]);

// Listen to navigation state changes
const listenerId = core.addStateListener((state) => {
  // Handle state updates
});

// Stop navigation
core.stopNavigation();

// Clean up
core.removeStateListener(listenerId);
```

## Key Components

### FerrostarCore

The main class that handles navigation logic, including:
- Route calculation
- Navigation state management
- Location updates
- Route deviation handling

### LocationProvider

Handles location updates using React Native's Geolocation API. You can implement custom location providers by implementing the `LocationProviderInterface`.

### RouteProvider

Manages route calculations and API interactions with Valhalla routing service.

### NavigationState

Represents the complete state of the navigation session, including:
- Trip state
- Route geometry
- Calculation status

## Advanced Features

### Route Deviation Handling

```typescript
core.deviationHandler = {
  correctiveActionForDeviation: (core, deviationMeters, remainingWaypoints) => {
    // Custom logic for handling route deviations
    return CorrectiveAction.GetNewRoutes;
  }
};
```

### Alternative Routes Processing

```typescript
core.alternativeRouteProcessor = {
  loadedAlternativeRoutes: (core, routes) => {
    // Handle alternative routes
  }
};
```

## Contributing

Please visit our [GitHub repository](https://github.com/stadiamaps/ferrostar) to:
- Report issues
- Submit pull requests
- View source code

## License

BSD-3-Clause

## Support

For issues and feature requests, please [file an issue](https://github.com/stadiamaps/ferrostar/issues) on our GitHub repository.
