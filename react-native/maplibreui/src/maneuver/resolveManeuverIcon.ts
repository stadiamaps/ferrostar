import {
  DrivingSide,
  ManeuverModifier,
  ManeuverType,
  type VisualInstructionContent,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import type { IconType } from './_icons';

const ROUNDABOUT_TYPES = new Set<ManeuverType>([
  ManeuverType.Roundabout,
  ManeuverType.Rotary,
  ManeuverType.RoundaboutTurn,
  ManeuverType.ExitRoundabout,
  ManeuverType.ExitRotary,
]);

/**
 * Classify a roundabout exit angle using the same angular convention as OSRM:
 * zero is a U-turn, 180 degrees is straight, and values increase towards the
 * left. The driving side changes the circulation shown by the icon, not the
 * direction of the exit itself.
 */
export function roundaboutModifierFromDegrees(
  degrees: number
): ManeuverModifier | undefined {
  if (!Number.isFinite(degrees) || degrees < 0 || degrees > 360) {
    return undefined;
  }

  if (degrees === 0 || degrees === 360) return ManeuverModifier.UTurn;
  if (degrees < 60) return ManeuverModifier.SharpRight;
  if (degrees < 140) return ManeuverModifier.Right;
  if (degrees < 160) return ManeuverModifier.SlightRight;
  if (degrees <= 200) return ManeuverModifier.Straight;
  if (degrees <= 220) return ManeuverModifier.SlightLeft;
  if (degrees <= 300) return ManeuverModifier.Left;
  return ManeuverModifier.SharpLeft;
}

function iconTypeForManeuverType(type: ManeuverType): string {
  // OSRM's roundabout-turn is a roundabout entered and exited at one
  // intersection. It uses the same icon family as a regular roundabout.
  if (type === ManeuverType.RoundaboutTurn) return 'roundabout';
  return ManeuverType[type].toLowerCase();
}

export function resolveManeuverIcon(
  content: VisualInstructionContent,
  drivingSide: DrivingSide
): IconType | null {
  if (content.maneuverType === undefined) return null;

  const type = iconTypeForManeuverType(content.maneuverType);
  let modifier = content.maneuverModifier;

  if (
    ROUNDABOUT_TYPES.has(content.maneuverType) &&
    content.roundaboutExitDegrees !== undefined
  ) {
    modifier =
      roundaboutModifierFromDegrees(content.roundaboutExitDegrees) ?? modifier;
  }

  if (modifier !== undefined) {
    const modifierName = ManeuverModifier[modifier].toLowerCase();
    const icon = `${type}_${modifierName}`;

    // The bundled roundabout families include mirrored icons for left-hand
    // traffic. Other maneuver families use the same asset on both sides.
    if (
      modifier !== ManeuverModifier.UTurn &&
      drivingSide === DrivingSide.Left &&
      ROUNDABOUT_TYPES.has(content.maneuverType)
    ) {
      return `${icon}_drivingleft` as IconType;
    }
    if (modifier !== ManeuverModifier.UTurn) return icon as IconType;
  }

  return type as IconType;
}
