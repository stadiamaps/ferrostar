import { describe, expect, it, vi } from 'vitest';

vi.mock('@stadiamaps/ferrostar-uniffi-react-native', () => {
  const numericEnum = (names: string[]) =>
    Object.fromEntries(
      names.flatMap((name, index) => [
        [name, index],
        [index, name],
      ])
    );

  return {
    DrivingSide: numericEnum(['Left', 'Right']),
    ManeuverModifier: numericEnum([
      'UTurn',
      'SharpRight',
      'Right',
      'SlightRight',
      'Straight',
      'SlightLeft',
      'Left',
      'SharpLeft',
    ]),
    ManeuverType: numericEnum([
      'Turn',
      'NewName',
      'Depart',
      'Arrive',
      'Merge',
      'OnRamp',
      'OffRamp',
      'Fork',
      'EndOfRoad',
      'Continue',
      'Roundabout',
      'Rotary',
      'RoundaboutTurn',
      'Notification',
      'ExitRoundabout',
      'ExitRotary',
    ]),
  };
});

import {
  DrivingSide,
  ManeuverModifier,
  ManeuverType,
  type VisualInstructionContent,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import {
  resolveManeuverIcon,
  roundaboutModifierFromDegrees,
} from '../resolveManeuverIcon';

function instruction(
  overrides: Partial<VisualInstructionContent> = {}
): VisualInstructionContent {
  return {
    text: '',
    maneuverType: ManeuverType.Roundabout,
    maneuverModifier: ManeuverModifier.Left,
    laneInfo: undefined,
    exitNumbers: [],
    ...overrides,
  };
}

describe('roundaboutModifierFromDegrees', () => {
  it.each([
    [0, ManeuverModifier.UTurn],
    [1, ManeuverModifier.SharpRight],
    [59, ManeuverModifier.SharpRight],
    [60, ManeuverModifier.Right],
    [139, ManeuverModifier.Right],
    [140, ManeuverModifier.SlightRight],
    [159, ManeuverModifier.SlightRight],
    [160, ManeuverModifier.Straight],
    [180, ManeuverModifier.Straight],
    [200, ManeuverModifier.Straight],
    [201, ManeuverModifier.SlightLeft],
    [220, ManeuverModifier.SlightLeft],
    [221, ManeuverModifier.Left],
    [300, ManeuverModifier.Left],
    [301, ManeuverModifier.SharpLeft],
    [359, ManeuverModifier.SharpLeft],
    [360, ManeuverModifier.UTurn],
  ])('maps %i degrees to modifier %i', (degrees, expected) => {
    expect(roundaboutModifierFromDegrees(degrees)).toBe(expected);
  });

  it.each([-1, 361, Number.NaN, Number.POSITIVE_INFINITY])(
    'rejects an invalid angle: %s',
    (degrees) => {
      expect(roundaboutModifierFromDegrees(degrees)).toBeUndefined();
    }
  );
});

describe('resolveManeuverIcon', () => {
  it('uses roundabout degrees instead of an incorrect modifier', () => {
    expect(
      resolveManeuverIcon(
        instruction({ roundaboutExitDegrees: 177 }),
        DrivingSide.Left
      )
    ).toBe('roundabout_straight_drivingleft');
  });

  it.each([
    [90, 'roundabout_right'],
    [150, 'roundabout_slightright'],
    [180, 'roundabout_straight'],
    [210, 'roundabout_slightleft'],
    [270, 'roundabout_left'],
  ] as const)(
    'selects the right-driving icon for %i degrees',
    (degrees, icon) => {
      expect(
        resolveManeuverIcon(
          instruction({ roundaboutExitDegrees: degrees }),
          DrivingSide.Right
        )
      ).toBe(icon);
    }
  );

  it.each([
    [ManeuverType.Roundabout, 'roundabout_straight_drivingleft'],
    [ManeuverType.Rotary, 'rotary_straight_drivingleft'],
    [ManeuverType.RoundaboutTurn, 'roundabout_straight_drivingleft'],
    [ManeuverType.ExitRoundabout, 'exitroundabout_straight_drivingleft'],
    [ManeuverType.ExitRotary, 'exitrotary_straight_drivingleft'],
  ] as const)('uses degrees for roundabout type %i', (maneuverType, icon) => {
    expect(
      resolveManeuverIcon(
        instruction({ maneuverType, roundaboutExitDegrees: 180 }),
        DrivingSide.Left
      )
    ).toBe(icon);
  });

  it('falls back to the provider modifier when degrees are unavailable', () => {
    expect(resolveManeuverIcon(instruction(), DrivingSide.Left)).toBe(
      'roundabout_left_drivingleft'
    );
  });

  it('does not apply roundabout degrees to other maneuver types', () => {
    expect(
      resolveManeuverIcon(
        instruction({
          maneuverType: ManeuverType.Turn,
          maneuverModifier: ManeuverModifier.Right,
          roundaboutExitDegrees: 180,
        }),
        DrivingSide.Left
      )
    ).toBe('turn_right');
  });

  it('falls back to the generic roundabout icon for a U-turn exit', () => {
    expect(
      resolveManeuverIcon(
        instruction({ roundaboutExitDegrees: 360 }),
        DrivingSide.Right
      )
    ).toBe('roundabout');
  });

  it('returns null when no maneuver type is available', () => {
    expect(
      resolveManeuverIcon(
        instruction({ maneuverType: undefined }),
        DrivingSide.Right
      )
    ).toBeNull();
  });
});
