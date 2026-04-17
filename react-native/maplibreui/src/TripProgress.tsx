import {
  StyleSheet,
  type ViewStyle,
  View,
  Pressable,
  Text,
} from 'react-native';
import {
  LocalizedDurationFormatter,
  LocalizedDistanceFormatter,
} from './_utils';
import { getIcon } from './maneuver/_icons';
import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';

type TripProgressViewProps = {
  style?: ViewStyle;
  fromDate?: Date;
};

const DurationFormatter = LocalizedDurationFormatter();
const DistanceFormatter = LocalizedDistanceFormatter();

export const TripProgress = ({
  fromDate = new Date(),
}: TripProgressViewProps) => {
  const core = useFerrostar();
  const { progress } = useNavigationState(core);

  if (progress === undefined) return;

  const estimatedArrival = new Date(
    fromDate.getTime() + progress.durationRemaining * 1000
  );

  const handleExit = () => {
    core.stopNavigation();
  };

  return (
    <View style={defaultStyle.container}>
      <View style={defaultStyle.box}>
        <View style={defaultStyle.centerBox}>
          <View>
            <Text style={defaultStyle.text}>
              {estimatedArrival.toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
              })}
            </Text>
          </View>
          <View>
            <Text style={defaultStyle.text}>
              {DurationFormatter.format(progress.durationRemaining)}
            </Text>
          </View>
          <View>
            <Text style={defaultStyle.text}>
              {DistanceFormatter.format(progress.distanceRemaining)}
            </Text>
          </View>
        </View>
        <Pressable style={defaultStyle.tapExit} onPress={handleExit}>
          <Text style={defaultStyle.text}>
            {getIcon('close', 24, 24, '#FFF')}
          </Text>
        </Pressable>
      </View>
    </View>
  );
};

const defaultStyle = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    margin: 10,
  },
  box: {
    flex: 1,
    flexDirection: 'row',
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 100,
    padding: 10,
  },
  centerBox: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginHorizontal: 30,
  },
  // Full circle button
  tapExit: {
    backgroundColor: '#52525b',
    borderRadius: 100,
    width: 48,
    height: 48,
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#000',
  },
});
