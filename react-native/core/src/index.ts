import { FerrostarCore, NavigationState } from './FerrostarCore';
import { NavigationUiState } from './NavigationUiState';
import {
  ManualLocationProvider,
  SimulatedLocationProvider,
  type LocationUpdateListener,
  type LocationProviderInterface,
} from './LocationProvider';
import { type RouteProvider, type RouteProviderAdapter, type RouteProviderCustom } from './RouteProvider';
import { type RouteDeviationHandler } from './RouteDeviationHandler';
import { type AlternativeRouteProcessor } from './AlternativeRouteProcessor';
import {
  InvalidStatusCodeException,
  NoRequestBodyException,
  NoResponseBodyException,
} from './FerrostarCoreException';

export {
  type Route,
  type TripProgress,
  type UserLocation,
  type Waypoint,
  type GeographicCoordinate,
  WaypointKind,
  WellKnownRouteProvider,
  type NavigationControllerConfig,
  NavigationController,
  type TripState,
  type NavState,
  RouteAdapter,
  LocationBias,
} from '@stadiamaps/ferrostar-uniffi-react-native';
export { FerrostarCore, NavigationUiState, NavigationState };
export {
  ManualLocationProvider,
  SimulatedLocationProvider,
  type LocationUpdateListener,
  type LocationProviderInterface,
};
export { type RouteProvider, type RouteProviderAdapter, type RouteProviderCustom };
export { type RouteDeviationHandler };
export { type AlternativeRouteProcessor };
export { useNavigationState } from './hooks/useNavigationState';
export { useFerrostar } from './hooks/useFerrostar';
export { useRoutes } from './hooks/useRoutes';
export {
  InvalidStatusCodeException,
  NoRequestBodyException,
  NoResponseBodyException,
};
