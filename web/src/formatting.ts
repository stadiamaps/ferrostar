import {
  DistanceSystem,
  LocalizedDistanceFormatter,
} from "@maptimy/platform-formatters";

const DistanceFormatter = LocalizedDistanceFormatter();

export function formatDistance(
  distanceMeters: number,
  system: DistanceSystem = "metric",
  desiredMaxDecimalPlaces: number = 2,
): string {
  const THRESHOLDS: Record<DistanceSystem, number> = {
    metric: 1000,
    imperial: 1609.34,           // 1 mile
    imperialWithYards: 1609.34,  // 1 mile
  };

  const exceedsThreshold = distanceMeters > THRESHOLDS[system];
  const decimalPlaces = exceedsThreshold ? desiredMaxDecimalPlaces : 0;

  return DistanceFormatter.format(distanceMeters, system, decimalPlaces);
}
