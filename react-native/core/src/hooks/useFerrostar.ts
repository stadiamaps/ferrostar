import { useMemo } from 'react';
import type { NavigationControllerConfig } from '@stadiamaps/ferrostar-uniffi-react-native';
import type { RouteProvider } from '../RouteProvider';
import { FerrostarCore } from '../FerrostarCore';
import { ManualLocationProvider, type LocationProviderInterface } from '../LocationProvider';

export function useFerrostar(
  config: NavigationControllerConfig,
  routeProvider: RouteProvider,
  locationProvider?: LocationProviderInterface
) {
  const core = useMemo(() => {
    return new FerrostarCore(
      config,
      locationProvider ?? new ManualLocationProvider(),
      routeProvider
    );
  }, [config, routeProvider, locationProvider]);

  return core;
}

