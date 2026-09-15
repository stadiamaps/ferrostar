import { StyleSheet, useWindowDimensions, View } from 'react-native';
import { BottomContainer } from './BottomContainer';
import { CurrentRoadName } from './CurrentRoadName';
import { InstructionsBanner } from './InstructionsBanner';
import { MapControls } from './MapControls';
import { TripProgress } from './TripProgress';
import {
  resolveNavigationViewLayout,
  type NavigationViewLayout,
} from './NavigationViewLayout';

type NavigationOverlayProps = {
  layout: NavigationViewLayout;
  onStopNavigation?: () => void;
};

export const NavigationOverlay = ({
  layout,
  onStopNavigation,
}: NavigationOverlayProps) => {
  const { width, height } = useWindowDimensions();
  const resolvedLayout = resolveNavigationViewLayout(layout, width, height);

  if (resolvedLayout === 'landscape') {
    return (
      <View pointerEvents="box-none" style={styles.overlay}>
        <View pointerEvents="box-none" style={styles.leadingColumn}>
          <InstructionsBanner />
          <BottomContainer>
            <TripProgress onStopNavigation={onStopNavigation} />
          </BottomContainer>
        </View>
        <View pointerEvents="box-none" style={styles.trailingColumn}>
          <MapControls />
          <BottomContainer>
            <CurrentRoadName />
          </BottomContainer>
        </View>
      </View>
    );
  }

  return (
    <>
      <InstructionsBanner />
      <MapControls />
      <BottomContainer>
        <CurrentRoadName />
        <TripProgress onStopNavigation={onStopNavigation} />
      </BottomContainer>
    </>
  );
};

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFillObject,
    flexDirection: 'row',
  },
  leadingColumn: {
    width: '50%',
  },
  trailingColumn: {
    width: '50%',
  },
});
