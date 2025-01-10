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
import InstructionsView from './InstructionsView';
import MapControls from './MapControls';

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

  // We need to find a way to override the location manager from within maplibre-react-native
  // or we need to create a custom puck that can have a custom navigation when navigating.
  // But that is only when the snapToRouteLocation is true.
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
      <MapView compassEnabled={false} attributionEnabled={false} {...props}>
        {isNavigating ? (
          <>
            <NavigationMapViewCamera />
            <UserLocation
              renderMode="native"
              androidRenderMode="gps"
              animated
            />
          </>
        ) : (
          <>
            <Camera followUserLocation />
            <UserLocation renderMode="native" />
          </>
        )}
        <BorderedPolyline points={uiState?.routeGeometry ?? []} zIndex={0} />
        {children}
      </MapView>
      <InstructionsView
        instructions={uiState?.visualInstruction}
        remainingSteps={uiState?.remainingSteps}
        distanceToNextManeuver={uiState?.progress?.distanceToNextManeuver ?? 0}
      />
      <MapControls />
      <TripProgressView
        progress={uiState?.progress}
        onTapExit={() => core.stopNavigation()}
      />
    </View>
  );
};

export default NavigationView;
