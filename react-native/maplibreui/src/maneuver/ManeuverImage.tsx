import { useMemo } from 'react';
import {
  ManeuverModifier,
  ManeuverType,
  type VisualInstructionContent,
  DrivingSide,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import { StyleSheet, View } from 'react-native';
import { getIcon, hasIcon, type IconType } from './_icons';
import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';

type ManeuverImageProps = {
  content: VisualInstructionContent;
};

export const ManeuverImage = ({ content }: ManeuverImageProps) => {
  const core = useFerrostar();
  const { drivingSide } = useNavigationState(core);

  const maneuverIcon: IconType | null = useMemo(() => {
    let modifier: string | null = null;
    let type: string | null = null;

    if (content.maneuverModifier !== undefined) {
      modifier = ManeuverModifier[content.maneuverModifier].toLowerCase();
    }

    if (content.maneuverType !== undefined) {
      type = ManeuverType[content.maneuverType].toLowerCase();
    }

    if (type === null) return null;
    if (modifier === null) return `${type}` as IconType;
    if (
      drivingSide === DrivingSide.Left &&
      hasIcon(`${type}_${modifier}_drivingleft`)
    ) {
      return `${type}_${modifier}_drivingleft` as IconType;
    }
    return `${type}_${modifier}` as IconType;
  }, [content.maneuverModifier, content.maneuverType, drivingSide]);

  if (maneuverIcon === null) return null;

  return <View style={style.text}>{getIcon(maneuverIcon, 60, 60)}</View>;
};

const style = StyleSheet.create({
  text: {
    width: 60,
    height: 60,
    marginRight: 10,
  },
});

export default ManeuverImage;
