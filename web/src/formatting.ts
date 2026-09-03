import {
  DistanceSystem,
  LocalizedDistanceFormatter,
} from "@maptimy/platform-formatters";

const DistanceFormatter = LocalizedDistanceFormatter();

const THRESHOLDS: Record<DistanceSystem, number> = {
  metric: 1000,
  imperial: 289,
  imperialWithYards: 300,
};

const METERS_PER_MILE = 1609.344;

const METERS_PER_LARGE_UNIT: Record<DistanceSystem, number> = {
  metric: 1000,
  imperial: METERS_PER_MILE,
  imperialWithYards: METERS_PER_MILE,
};

const FRACTIONAL_LARGE_UNIT_MAX = 10;

export function formatDistance(
  distanceMeters: number,
  system: DistanceSystem = "metric",
  desiredMaxDecimalPlaces: number = 2,
): string {
  const exceedsThreshold = distanceMeters > THRESHOLDS[system];
  const largeUnitDistance = distanceMeters / METERS_PER_LARGE_UNIT[system];
  const decimalPlaces =
    exceedsThreshold && largeUnitDistance <= FRACTIONAL_LARGE_UNIT_MAX
      ? desiredMaxDecimalPlaces
      : 0;

  return DistanceFormatter.format(distanceMeters, system, decimalPlaces);
}
