import MapLibreGL, {
  Camera,
  MapView,
  UserLocation,
} from '@maplibre/maplibre-react-native';
import { useEffect, useMemo, useState, type ComponentProps } from 'react';
import { FerrostarCore } from '../core/FerrostarCore';
import { NavigationUiState } from '../core/NavigationUiState';
import BorderedPolyline from './BorderedPolyline';
import NavigationMapViewCamera from './NavigationMapViewCamera';
import TripProgressView from './TripProgressView';
import { View } from 'react-native';

MapLibreGL.setAccessToken(null);

type NavigationViewProps = ComponentProps<typeof MapView> & {
  core: FerrostarCore;
  snapUserLocationToRoute?: boolean;
};

const NavigationView = (props: NavigationViewProps) => {
  const { core, children, snapUserLocationToRoute = true } = props;
  const [uiState, setUiState] = useState<NavigationUiState>();

  const isNavigating = useMemo(() => {
    return uiState?.isNavigating() ?? false;
  }, [uiState]);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const location = useMemo(() => {
    if (snapUserLocationToRoute && isNavigating) {
      return uiState?.snappedLocation;
    }

    return uiState?.location;
  }, [
    isNavigating,
    snapUserLocationToRoute,
    uiState?.location,
    uiState?.snappedLocation,
  ]);

  useEffect(() => {
    const watchId = core.addStateListener((state) => {
      setUiState(NavigationUiState.fromFerrostar(state));
    });

    return () => {
      core.removeStateListener(watchId);
    };
  }, [core]);

  return (
    <View style={{ flex: 1, position: 'relative' }}>
      <MapView compassEnabled={false} {...props}>
        {isNavigating ? (
          <NavigationMapViewCamera />
        ) : (
          <>
            <Camera followUserLocation />
            <UserLocation />
          </>
        )}
        <BorderedPolyline points={uiState?.routeGeometry ?? []} zIndex={0} />
        {children}
      </MapView>
      <TripProgressView
        progress={uiState?.progress}
        onTapExit={() => core.stopNavigation()}
      />
    </View>
  );
};

export default NavigationView;
