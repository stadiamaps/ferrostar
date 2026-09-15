import { describe, expect, it, vi } from 'vitest';
import { decodeAnnotation } from '../Annotation';
import { parseValhallaExtendedOSRMAnnotation } from '../ValhallaExtendedOSRMAnnotation';

describe('decodeAnnotation', () => {
  it('does not invoke the parser when no annotation is available', () => {
    const parser = vi.fn();

    expect(decodeAnnotation(undefined, parser)).toEqual({});
    expect(parser).not.toHaveBeenCalled();
  });

  it('decodes JSON before invoking the parser', () => {
    const parser = vi.fn((value: unknown) => value);

    expect(decodeAnnotation('{"speed":12.5}', parser)).toEqual({
      data: { speed: 12.5 },
    });
    expect(parser).toHaveBeenCalledWith({ speed: 12.5 });
  });

  it('returns malformed JSON as an error', () => {
    const result = decodeAnnotation('{', (value) => value);

    expect(result.data).toBeUndefined();
    expect(result.error).toBeInstanceOf(SyntaxError);
  });

  it('returns parser failures without replacing their error', () => {
    const expected = new TypeError('invalid annotation');
    const result = decodeAnnotation('{}', () => {
      throw expected;
    });

    expect(result).toEqual({ error: expected });
  });
});

describe('parseValhallaExtendedOSRMAnnotation', () => {
  it('parses the extended OSRM fields and ignores extensions', () => {
    expect(
      parseValhallaExtendedOSRMAnnotation({
        maxspeed: { speed: 50, unit: 'km/h' },
        speed: 12.5,
        distance: 20,
        duration: 1.6,
        congestion: 'low',
      })
    ).toEqual({
      speedLimit: { type: 'value', value: 50, unit: 'km/h' },
      speed: 12.5,
      distance: 20,
      duration: 1.6,
    });
  });

  it.each([
    [{ none: true }, { type: 'noLimit' }],
    [{ unknown: true }, { type: 'unknown' }],
  ] as const)('parses the maxspeed variant %#', (maxspeed, expected) => {
    expect(parseValhallaExtendedOSRMAnnotation({ maxspeed })).toEqual({
      speedLimit: expected,
    });
  });

  it('treats null optional fields as unavailable', () => {
    expect(
      parseValhallaExtendedOSRMAnnotation({
        maxspeed: null,
        speed: null,
        distance: null,
        duration: null,
      })
    ).toEqual({});
  });

  it.each([
    null,
    [],
    { speed: 'fast' },
    { maxspeed: { speed: 50, unit: 'meters-per-second' } },
  ])('rejects an invalid annotation: %j', (annotation) => {
    expect(() => parseValhallaExtendedOSRMAnnotation(annotation)).toThrow(
      TypeError
    );
  });
});
