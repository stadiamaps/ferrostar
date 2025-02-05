import {
  RouteDeviation,
  RouteStep,
  TripProgress,
  TripState,
  UserLocation,
  VisualInstruction,
} from "ferrostar-rn-uniffi";

export function getNanoTime(): number {
  const hrTime = process.hrtime();
  return hrTime[0] * 1000000000 + hrTime[1];
}

export function ab2json(ab: ArrayBuffer): object {
  return JSON.parse(
    String.fromCharCode.apply(null, Array.from(new Uint8Array(ab))),
  );
}

export function currentRoadName(tripState: TripState): string | undefined {
  if (TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.remainingSteps[0]?.roadName;
  } else {
    return undefined;
  }
}

export function visualInstruction(
  tripState: TripState,
): VisualInstruction | undefined {
  if (TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.visualInstruction;
  } else {
    return undefined;
  }
}

export function deviation(tripState: TripState): RouteDeviation | undefined {
  if (TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.deviation;
  } else {
    return undefined;
  }
}

export function progress(tripState: TripState): TripProgress | undefined {
  if (TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.progress;
  } else {
    return undefined;
  }
}

export function remainingSteps(tripState: TripState): RouteStep[] | undefined {
  if (TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.remainingSteps;
  } else {
    return undefined;
  }
}

export function snappedUserLocation(
  tripState: TripState,
  fallback?: UserLocation,
): UserLocation | undefined {
  if (TripState.Navigating.instanceOf(tripState)) {
    return tripState.inner.snappedUserLocation;
  } else {
    return fallback;
  }
}
