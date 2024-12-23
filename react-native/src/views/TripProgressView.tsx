import {
  StyleSheet,
  type ViewStyle,
  View,
  Pressable,
  Text,
} from 'react-native';
import type { TripProgress } from '../generated/ferrostar';
import {
  LocalizedDurationFormatter,
  LocalizedDistanceFormatter,
} from './_utils';

type TripProgressViewProps = {
  progress?: TripProgress;
  style?: ViewStyle;
  fromDate?: Date;
  onTapExit: () => void | null;
};

const DurationFormatter = LocalizedDurationFormatter();
const DistanceFormatter = LocalizedDistanceFormatter();

const TripProgressView = ({
  progress,
  fromDate = new Date(),
  onTapExit,
}: TripProgressViewProps) => {
  if (progress === undefined) return;

  // TODO: fix this
  const estimatedArrival = new Date(
    fromDate.getTime() +
      progress.distanceRemaining * 1000 +
      fromDate.getTimezoneOffset() * 60 * 1000
  );

  return (
    <View style={defaultStyle.container}>
      <View style={defaultStyle.box}>
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
        {onTapExit != null && (
          <Pressable style={defaultStyle.tapExit} onPress={onTapExit}>
            <Text style={defaultStyle.text}>X</Text>
          </Pressable>
        )}
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
    justifyContent: 'space-between',
    borderRadius: 100,
    padding: 10,
  },
  // Full circle button
  tapExit: {
    backgroundColor: '#d3d3d3',
    borderRadius: 100,
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#000',
  },
});

export default TripProgressView;
