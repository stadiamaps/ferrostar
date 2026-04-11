import {
  WellKnownRouteProvider,
  type Waypoint,
  type UserLocation,
  type Route,
} from '@stadiamaps/ferrostar-uniffi-react-native';

/**
 * Reworked RouteProvider type structure to align with Android and bindings conventions.
 */

// Define RouteProvider interface/type that corresponds to Android's interface and usages
// Since Kotlin can pass around interfaces and sealed classes, in TS we use a union of types
// to represent the different variants that can be passed to FerrostarCore.

export type RouteProviderAdapter = {
  kind: 'adapter';
  provider: WellKnownRouteProvider;
};

export type RouteProviderCustom = {
  kind: 'custom';
  getRoutes(
    userLocation: UserLocation,
    waypoints: Array<Waypoint>
  ): Promise<Array<Route>>;
};

export type RouteProvider = RouteProviderAdapter | RouteProviderCustom;

/**
 * TypeScript helper equivalent to Android's `withJsonOptions` extension.
 * Merges a record into the JSON options of a well-known provider.
 *
 * @param provider The well-known provider to modify.
 * @param options A record containing additional options.
 * @returns A new WellKnownRouteProvider with the options merged.
 */
export function withJsonOptions(
  provider: WellKnownRouteProvider,
  options?: Record<string, unknown>
): WellKnownRouteProvider {
  let existingOptions: Record<string, unknown>;
  const currentOptionsJson = WellKnownRouteProvider.Valhalla.instanceOf(
    provider
  )
    ? provider.inner.optionsJson
    : WellKnownRouteProvider.GraphHopper.instanceOf(provider)
      ? provider.inner.optionsJson
      : undefined;

  try {
    existingOptions = JSON.parse(currentOptionsJson ?? '{}');
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
  } catch (_: unknown) {
    existingOptions = {};
  }

  const mergedOptions = { ...existingOptions, ...options };
  const mergedOptionsJson = JSON.stringify(mergedOptions);

  if (WellKnownRouteProvider.Valhalla.instanceOf(provider)) {
    return WellKnownRouteProvider.Valhalla.new({
      endpointUrl: provider.inner.endpointUrl,
      profile: provider.inner.profile,
      optionsJson: mergedOptionsJson,
    });
  }

  if (WellKnownRouteProvider.GraphHopper.instanceOf(provider)) {
    return WellKnownRouteProvider.GraphHopper.new({
      ...provider.inner,
      optionsJson: mergedOptionsJson,
    });
  }

  // Should be unreachable if WellKnownRouteProvider is exhaustive
  return provider;
}
