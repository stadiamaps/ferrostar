import { useMemo } from 'react';
import {
  ManeuverModifier,
  ManeuverType,
  type VisualInstructionContent,
} from '../../generated/ferrostar';
import { StyleSheet, View } from 'react-native';
import { getIcon, type IconType } from './_icons';

type ManeuverImageProps = {
  content: VisualInstructionContent;
};

export const ManeuverImage = ({ content }: ManeuverImageProps) => {
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
    return `${type}_${modifier}` as IconType;
  }, [content.maneuverModifier, content.maneuverType]);

  if (maneuverIcon === null) return null;

  return <View style={style.text}>{getIcon(maneuverIcon, 48, 48)}</View>;
};

const style = StyleSheet.create({
  text: {
    width: 48,
    height: 48,
    marginRight: 10,
  },
});

export default ManeuverImage;
