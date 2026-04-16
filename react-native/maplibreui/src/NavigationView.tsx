import {
  Camera,
  type CameraRef,
  type MapRef,
  Map,
  UserLocation,
  type ViewStateChangeEvent,
} from '@maplibre/maplibre-react-native';
import { bbox } from '@turf/bbox';
import { ComponentProps, useState, useRef, useMemo, useCallback } from 'react';
import { type NativeSyntheticEvent, StyleSheet, View } from 'react-native';
import {
  FerrostarCore,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';
import { BorderedPolyline } from './BorderedPolyline';
import {
  NavigationActivity,
  NavigationMapViewCamera,
} from './NavigationMapViewCamera';
import { TripProgressView } from './TripProgressView';
import { InstructionsView } from './InstructionsView';
import { MapControls } from './MapControls';
import { NavigationPuck } from './NavigationPuck';

type NavigationViewProps = ComponentProps<typeof Map> & {
  core: FerrostarCore;
  activity?: NavigationActivity;
  snapUserLocationToRoute?: boolean;
};

export const NavigationView = (props: NavigationViewProps) => {
  const {
    core,
    children,
    activity = NavigationActivity.Automotive,
    snapUserLocationToRoute = true,
  } = props;
  const mapRef = useRef<MapRef>(null);
  const cameraRef = useRef<CameraRef>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [cameraMode, setCameraMode] = useState<
    'following' | 'overview' | 'detached'
  >('following');

  const uiState = useNavigationState(core, isMuted);

  const isNavigating = useMemo(() => {
    return uiState?.isNavigating() ?? false;
  }, [uiState]);

  const handleMute = () => {
    setIsMuted((prev) => !prev);
  };

  const handleRoutePress = useCallback(() => {
    if (!uiState) return;

    if (cameraMode === 'overview') {
      setCameraMode('following');
      return;
    }

    if (
      uiState.routeGeometry === undefined ||
      uiState.routeGeometry.length === 0
    ) {
      return;
    }

    const lineString = {
      type: 'Feature',
      properties: {},
      geometry: {
        type: 'LineString',
        coordinates: uiState.routeGeometry.map((point) => [
          point.lng,
          point.lat,
        ]),
      },
    };
    const [west, south, east, north] = bbox(lineString);

    setCameraMode('overview');
    cameraRef.current.fitBounds([west, south, east, north], {
      pitch: 0,
      padding: { top: 20, right: 20, bottom: 20, left: 20 },
    });
  }, [cameraMode, uiState]);

  const handleRecenterPress = useCallback(() => {
    cameraRef.current.zoomTo(activity.zoom, { pitch: activity.pitch });
    setCameraMode('following');
  }, [activity]);

  const handleCameraChanged = useCallback(
    (event: NativeSyntheticEvent<ViewStateChangeEvent>) => {
      if (cameraMode === 'following' && event.nativeEvent.userInteraction) {
        setCameraMode('detached');
      }
    },
    [cameraMode]
  );

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

  return (
    <View style={defaultStyle.container}>
      <Map
        ref={mapRef}
        compass={false}
        attribution={false}
        onRegionIsChanging={handleCameraChanged}
        {...props}
      >
        {isNavigating ? (
          <>
            <NavigationMapViewCamera
              ref={cameraRef}
              activity={activity}
              followUserLocation={
                cameraMode === 'following' ? location : undefined
              }
            />
            <NavigationPuck location={location} />
          </>
        ) : (
          <>
            <Camera ref={cameraRef} trackUserLocation="default" zoom={10} />
            <UserLocation />
          </>
        )}

        <BorderedPolyline points={uiState?.routeGeometry ?? []} zIndex={0} />
        {children}
      </Map>
      <InstructionsView
        instructions={uiState?.visualInstruction}
        remainingSteps={uiState?.remainingSteps}
        distanceToNextManeuver={uiState?.progress?.distanceToNextManeuver ?? 0}
      />
      <MapControls
        isNavigating={isNavigating}
        isMuted={uiState?.isMuted ?? false}
        cameraMode={cameraMode}
        onRoutePress={handleRoutePress}
        onRecenterPress={handleRecenterPress}
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
