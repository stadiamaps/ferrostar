import {
  Camera,
  type CameraRef,
  type MapViewRef,
  MapView,
  UserLocation,
  setAccessToken,
} from '@maplibre/maplibre-react-native';
import { bbox } from '@turf/bbox';
import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import {
  FerrostarCore,
  NavigationUiState,
  snappedUserLocation,
} from '@stadiamaps/ferrostar-core-react-native';
import BorderedPolyline from './BorderedPolyline';
import NavigationMapViewCamera from './NavigationMapViewCamera';
import TripProgressView from './TripProgressView';
import { StyleSheet, View } from 'react-native';
import InstructionsView from './InstructionsView';
import MapControls from './MapControls';

setAccessToken(null);

type NavigationViewProps = ComponentProps<typeof MapView> & {
  core: FerrostarCore;
  snapUserLocationToRoute?: boolean;
};

const NavigationView = (props: NavigationViewProps) => {
  const { core, children, snapUserLocationToRoute = true } = props;
  const mapRef = useRef<MapViewRef>(null);
  const cameraRef = useRef<CameraRef>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [routeBounds, setRouteBounds] = useState<{
    ne: [number, number];
    sw: [number, number];
  } | null>(null);
  const [uiState, setUiState] = useState<NavigationUiState>();

  const isNavigating = useMemo(() => {
    return uiState?.isNavigating() ?? false;
  }, [uiState]);

  const handleMute = () => {
    setIsMuted(!isMuted);
    if (uiState === undefined) return;

    setUiState(uiState.setMuted(!isMuted));
  };

  const handleRoutePress = useCallback(() => {
    if (!uiState) return;

    // If the route is already focused, we need to reset the camera to follow the user.
    if (routeBounds) {
      setRouteBounds(null);
      return;
    }

    if (
      uiState.routeGeometry === undefined ||
      uiState.routeGeometry.length === 0
    ) {
      return;
    }

    const lineString = {
      type: 'Feature' as const,
      properties: {},
      geometry: {
        type: 'LineString' as const,
        coordinates: uiState.routeGeometry.map((point) => [
          point.lng,
          point.lat,
        ]) as [number, number][],
      },
    };
    const [minX, minY, maxX, maxY] = bbox(lineString);
    const ne = [minX, minY] satisfies [number, number];
    const sw = [maxX, maxY] satisfies [number, number];

    setRouteBounds({ ne, sw });
  }, [routeBounds, uiState]);

  const handleZoom = useCallback(
    async (type: 'in' | 'out') => {
      if (!cameraRef.current || !mapRef.current) return;
      const zoom = await mapRef.current.getZoom();

      if (type === 'in') {
        cameraRef.current.zoomTo(zoom + 1);
        return;
      }

      cameraRef.current.zoomTo(zoom - 1);
    },
    [cameraRef]
  );

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
      const newState = NavigationUiState.fromFerrostar(
        state,
        isMuted,
        core.locationProvider.lastLocation
      );

      setUiState(newState);
    });

    return () => {
      core.removeStateListener(watchId);
    };
  }, []);

  return (
    <View style={defaultStyle.container}>
      <MapView
        ref={mapRef}
        compassEnabled={false}
        attributionEnabled={false}
        {...props}
      >
        {isNavigating ? (
          <>
            <NavigationMapViewCamera ref={cameraRef} bounds={routeBounds} />
            <UserLocation
              renderMode="native"
              androidRenderMode="gps"
              animated
            />
          </>
        ) : (
          <>
            <Camera ref={cameraRef} followUserLocation />
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
      <MapControls
        isNavigating={isNavigating}
        isMuted={uiState?.isMuted ?? false}
        onRoutePress={handleRoutePress}
        onMutePress={handleMute}
        onZoomIn={() => handleZoom('in')}
        onZoomOut={() => handleZoom('out')}
      />
      <TripProgressView
        progress={uiState?.progress}
        onTapExit={() => core.stopNavigation()}
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

export default NavigationView;
