import { useState, useCallback } from 'react';
import type { Route, UserLocation, Waypoint } from '@stadiamaps/ferrostar-uniffi-react-native';
import type { FerrostarCore } from '../FerrostarCore';

interface UseRoutesResult {
  routes: Array<Route>;
  isLoading: boolean;
  error: Error | null;
  fetchRoutes: (
    initialLocation: UserLocation,
    waypoints: Array<Waypoint>
  ) => Promise<Array<Route>>;
}

/**
 * A convenience hook for fetching routes from FerrostarCore.
 *
 * Handles the asynchronous state (loading, error, routes) so you don't have to
 * manually track it inside your components.
 */
export function useRoutes(core: FerrostarCore): UseRoutesResult {
  const [routes, setRoutes] = useState<Array<Route>>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchRoutes = useCallback(
    async (
      initialLocation: UserLocation,
      waypoints: Array<Waypoint>
    ): Promise<Array<Route>> => {
      setIsLoading(true);
      setError(null);
      try {
        const fetchedRoutes = await core.getRoutes(initialLocation, waypoints);
        setRoutes(fetchedRoutes);
        return fetchedRoutes;
      } catch (err) {
        const e = err instanceof Error ? err : new Error(String(err));
        setError(e);
        setRoutes([]);
        return [];
      } finally {
        setIsLoading(false);
      }
    },
    [core]
  );

  return { routes, isLoading, error, fetchRoutes };
}
