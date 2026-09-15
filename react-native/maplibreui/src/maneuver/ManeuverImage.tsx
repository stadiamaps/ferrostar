import { type VisualInstructionContent } from '@stadiamaps/ferrostar-uniffi-react-native';
import { StyleSheet, View } from 'react-native';
import { getIcon } from './_icons';
import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';
import { resolveManeuverIcon } from './resolveManeuverIcon';

type ManeuverImageProps = {
  content: VisualInstructionContent;
};

export const ManeuverImage = ({ content }: ManeuverImageProps) => {
  const core = useFerrostar();
  const { drivingSide } = useNavigationState(core);

  const maneuverIcon = resolveManeuverIcon(content, drivingSide);

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
