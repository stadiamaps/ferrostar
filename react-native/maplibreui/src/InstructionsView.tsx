import { useMemo, useState } from 'react';
import type { RouteStep, VisualInstruction } from '../generated/ferrostar';
import { LocalizedDistanceFormatter, type Formatter } from './_utils';
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';
import ManeuverImage from './maneuver/ManeuverImage';

export type InstructionViewProps = {
  instructions?: VisualInstruction;
  distanceToNextManeuver?: number;
  distanceFormatter?: Formatter;
  remainingSteps?: Array<RouteStep>;
};

/**
 * A banner view with sensible defaults.
 *
 * This banner view includes the default iconography from Mapbox, attempts to use the device's
 * locale for formatting distances and determining flow order (this can be overridden by passing a
 * customized formatter.)
 */
const InstructionsView = ({
  instructions,
  distanceToNextManeuver = 0,
  distanceFormatter = LocalizedDistanceFormatter(),
  remainingSteps,
}: InstructionViewProps) => {
  const { height } = useWindowDimensions();
  const [isExpanded, setIsExpanded] = useState(false);

  // These are the steps that will be listed in the dropdown menu
  const nextSteps = useMemo(() => {
    return remainingSteps?.slice(1) ?? [];
  }, [remainingSteps]);

  const upcomingInstructions = useMemo(() => {
    return nextSteps.map((step) => step.visualInstructions[0] ?? null);
  }, [nextSteps]);

  const instructionListHeight = useMemo(() => {
    return height * 0.7;
  }, [height]);

  const handleExpand = () => {
    setIsExpanded(!isExpanded);
  };

  if (!instructions) return null;

  return (
    <View style={defaultStyle.container}>
      <View style={defaultStyle.column}>
        <Pressable
          style={defaultStyle.instructionButton}
          onPress={handleExpand}
        >
          <View style={{ flex: 1, flexDirection: 'row' }}>
            <ManeuverImage content={instructions.primaryContent} />
            <View style={{ flex: 1 }}>
              <Text style={defaultStyle.distanceText}>
                {distanceFormatter.format(distanceToNextManeuver)}
              </Text>
              <Text style={defaultStyle.instructionText}>
                {instructions.primaryContent.text}
              </Text>
            </View>
          </View>
          <View style={defaultStyle.pill} />
        </Pressable>
      </View>
      {isExpanded && (
        <View style={defaultStyle.instructionList}>
          <FlatList
            style={{
              maxHeight: instructionListHeight,
              backgroundColor: '#fff',
              borderRadius: 10,
            }}
            data={upcomingInstructions}
            renderItem={({ item, index }) => {
              if (!item) return null;
              return (
                <View key={index} style={defaultStyle.instructionListItem}>
                  <ManeuverImage content={item.primaryContent} />
                  <View>
                    <Text style={defaultStyle.distanceText}>
                      {distanceFormatter.format(
                        item.triggerDistanceBeforeManeuver
                      )}
                    </Text>
                    <Text
                      style={defaultStyle.instructionText}
                      numberOfLines={1}
                    >
                      {item.primaryContent.text}
                    </Text>
                  </View>
                </View>
              );
            }}
          />
        </View>
      )}
    </View>
  );
};

const defaultStyle = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    flex: 1,
    flexDirection: 'column',
  },
  column: {
    flex: 1,
    flexDirection: 'column',
    backgroundColor: '#fff',
    borderRadius: 10,
    marginTop: 10,
    marginRight: 10,
    marginLeft: 10,
    height: 90,
  },
  instructionList: {
    flex: 1,
    flexDirection: 'column',
    borderRadius: 10,
    marginTop: 10,
    marginRight: 10,
    marginLeft: 10,
  },
  instructionButton: {
    flex: 1,
    flexDirection: 'column',
    padding: 10,
    alignItems: 'center',
  },
  instructionText: {
    flex: 1,
    fontSize: 18,
    color: '#000',
  },
  instructionListItem: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    margin: 10,
    paddingVertical: 10,
  },
  distanceText: {
    fontSize: 16,
    color: '#000',
  },
  pill: {
    flex: 1,
    width: 24,
    maxHeight: 2,
    borderRadius: 12,
    backgroundColor: '#000',
    alignSelf: 'center',
    marginTop: 10,
  },
});

export default InstructionsView;
