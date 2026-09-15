export type SpeedUnit = 'km/h' | 'mph' | 'knots';

export type SpeedLimit =
  | { type: 'noLimit' }
  | { type: 'unknown' }
  | { type: 'value'; value: number; unit: SpeedUnit };

/**
 * Attributes for the route segment at the user's current snapped position.
 */
export type ValhallaExtendedOSRMAnnotation = {
  /** The speed limit reported for the segment. */
  speedLimit?: SpeedLimit;
  /** The estimated travel speed, in meters per second. */
  speed?: number;
  /** The segment distance, in meters. */
  distance?: number;
  /** The estimated segment traversal time, in seconds. */
  duration?: number;
};

/**
 * Parse an annotation from Valhalla's extended OSRM response format.
 *
 * Unknown properties are ignored so providers can add extension fields without
 * breaking navigation. Known properties are validated before being exposed to
 * application code.
 */
export function parseValhallaExtendedOSRMAnnotation(
  value: unknown
): ValhallaExtendedOSRMAnnotation {
  const annotation = requireRecord(value, 'annotation');
  const result: ValhallaExtendedOSRMAnnotation = {};

  const speedLimit = parseSpeedLimit(annotation.maxspeed);
  if (speedLimit !== undefined) {
    result.speedLimit = speedLimit;
  }

  const speed = optionalNumber(annotation.speed, 'speed');
  if (speed !== undefined) {
    result.speed = speed;
  }

  const distance = optionalNumber(annotation.distance, 'distance');
  if (distance !== undefined) {
    result.distance = distance;
  }

  const duration = optionalNumber(annotation.duration, 'duration');
  if (duration !== undefined) {
    result.duration = duration;
  }

  return result;
}

function parseSpeedLimit(value: unknown): SpeedLimit | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }

  const speedLimit = requireRecord(value, 'maxspeed');

  if (speedLimit.none === true) {
    return { type: 'noLimit' };
  }

  if (speedLimit.unknown === true) {
    return { type: 'unknown' };
  }

  const speed = optionalNumber(speedLimit.speed, 'maxspeed.speed');
  const unit = speedLimit.unit;
  if (speed !== undefined && isSpeedUnit(unit)) {
    return { type: 'value', value: speed, unit };
  }

  throw new TypeError(
    'maxspeed must describe no limit, an unknown limit, or a value and unit'
  );
}

function optionalNumber(value: unknown, name: string): number | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new TypeError(`${name} must be a finite number`);
  }

  return value;
}

function requireRecord(value: unknown, name: string): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new TypeError(`${name} must be an object`);
  }

  return value as Record<string, unknown>;
}

function isSpeedUnit(value: unknown): value is SpeedUnit {
  return value === 'km/h' || value === 'mph' || value === 'knots';
}
