import {
  GeographicCoordinate,
  RouteDeviation,
  RouteStep,
  TripProgress,
  TripState,
  UserLocation,
  VisualInstruction,
} from '@stadiamaps/ferrostar-uniffi-react-native';

export function getNanoTime(): number {
  return performance.now() * 1000000;
}

export function getDistance(
  c1: GeographicCoordinate,
  c2: GeographicCoordinate
): number {
  const R = 6371e3; // meters
  const phi1 = (c1.lat * Math.PI) / 180;
  const phi2 = (c2.lat * Math.PI) / 180;
  const deltaPhi = ((c2.lat - c1.lat) * Math.PI) / 180;
  const deltaLambda = ((c2.lng - c1.lng) * Math.PI) / 180;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) *
      Math.cos(phi2) *
      Math.sin(deltaLambda / 2) *
      Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

export function ab2json(ab: ArrayBuffer): object {
  return JSON.parse(
    String.fromCharCode.apply(null, Array.from(new Uint8Array(ab)))
  );
}

export function currentRoadName(tripState?: TripState): string | undefined {
  if (tripState && TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.remainingSteps[0]?.roadName;
  } else {
    return undefined;
  }
}

export function visualInstruction(
  tripState?: TripState
): VisualInstruction | undefined {
  if (tripState && TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.visualInstruction;
  } else {
    return undefined;
  }
}

export function deviation(tripState?: TripState): RouteDeviation | undefined {
  if (tripState && TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.deviation;
  } else {
    return undefined;
  }
}

export function progress(tripState?: TripState): TripProgress | undefined {
  if (tripState && TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.progress;
  } else {
    return undefined;
  }
}

export function remainingSteps(tripState?: TripState): RouteStep[] | undefined {
  if (tripState && TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.remainingSteps;
  } else {
    return undefined;
  }
}

export function snappedUserLocation(
  tripState?: TripState,
  fallback?: UserLocation
): UserLocation | undefined {
  if (tripState && TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.snappedUserLocation;
  } else {
    return fallback;
  }
}

export function preferredUserLocation(
  tripState?: TripState,
  location: UserLocation
): UserLocation {
  if (tripState && TripState.Navigating.instanceOf(tripState)) {
    if (tripState.inner.deviation.tag === 'NoDeviation') {
      return tripState.inner.snappedUserLocation;
    }
    if (tripState.inner.deviation.tag === 'OffRoute') {
      return tripState.inner.userLocation;
    }
  }

  return location;
}
