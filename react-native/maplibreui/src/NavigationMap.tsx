import { Map, UserLocation, Camera } from '@maplibre/maplibre-react-native';
import { ComponentProps } from 'react';
import { StyleSheet, View } from 'react-native';
import { BorderedPolyline } from './BorderedPolyline';
import { NavigationCamera } from './NavigationCamera';
import { NavigationPuck } from './NavigationPuck';
import { Navigating } from './Navigating';
import { useCamera } from './hooks/useCamera';
import { NotNavigating } from './NotNavigating';
import { NavigationOverlay } from './NavigationOverlay';
import type { NavigationViewLayout } from './NavigationViewLayout';

export type NavigationMapProps = ComponentProps<typeof Map> & {
  /**
   * Controls how navigation UI is arranged. Dynamic layout follows the window dimensions.
   * @default 'dynamic'
   */
  layout?: NavigationViewLayout;
  onStopNavigation?: () => void;
};

/**
 * Map component that supports navigation UI elements such as the trip progress bar, instructions banner, and map controls.
 * @param props
 * @returns
 */
export const NavigationMap = (props: NavigationMapProps) => {
  const {
    children,
    layout = 'dynamic',
    onStopNavigation,
    ...mapProps
  } = props;
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
          <NavigationCamera layout={layout} />
          <NavigationPuck />
        </Navigating>
        <NotNavigating>
          <UserLocation />
          <Camera trackUserLocation="default" zoom={10} />
        </NotNavigating>
        <BorderedPolyline zIndex={0} />
        {children}
      </Map>
      <NavigationOverlay
        layout={layout}
        onStopNavigation={onStopNavigation}
      />
    </View>
  );
};

const defaultStyle = StyleSheet.create({
  container: {
    flex: 1,
    position: 'relative',
  },
});
