import { Map, UserLocation, Camera } from '@maplibre/maplibre-react-native';
import { ComponentProps } from 'react';
import { StyleSheet, View } from 'react-native';
import { BorderedPolyline } from './BorderedPolyline';
import { NavigationCamera } from './NavigationCamera';
import { TripProgress } from './TripProgress';
import { InstructionsBanner } from './InstructionsBanner';
import { MapControls } from './MapControls';
import { NavigationPuck } from './NavigationPuck';
import { Navigating } from './Navigating';
import { CurrentRoadName } from './CurrentRoadName';
import { useCamera } from './hooks/useCamera';
import { NotNavigating } from './NotNavigating';
import { BottomContainer } from './BottomContainer';

type NavigationMapProps = ComponentProps<typeof Map> & {
  onStopNavigation?: () => void;
};

/**
 * Map component that supports navigation UI elements such as the trip progress bar, instructions banner, and map controls.
 * @param props
 * @returns
 */
export const NavigationMap = (props: NavigationMapProps) => {
  const { children, onStopNavigation, ...mapProps } = props;
  const { cameraChange } = useCamera();

  return (
    <View style={defaultStyle.container}>
      <Map
        compass={false}
        attribution={false}
        onRegionIsChanging={cameraChange}
        {...mapProps}
      >
        <Navigating>
          <NavigationCamera />
          <NavigationPuck />
        </Navigating>
        <NotNavigating>
          <UserLocation />
          <Camera trackUserLocation="default" zoom={10} />
        </NotNavigating>
        <BorderedPolyline zIndex={0} />
        {children}
      </Map>
      <InstructionsBanner />
      <MapControls />
      <BottomContainer>
        <CurrentRoadName />
        <TripProgress onStopNavigation={onStopNavigation} />
      </BottomContainer>
    </View>
  );
};

const defaultStyle = StyleSheet.create({
  container: {
    flex: 1,
    position: 'relative',
  },
});
