import { StyleSheet, View, Button, Text } from 'react-native';
import {
  CourseFiltering,
  RouteDeviationTracking,
  stepAdvanceDistanceEntryAndExit,
  stepAdvanceDistanceToEndOfStep,
  WaypointAdvanceMode,
  WaypointKind,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import { FerrostarCore } from '@stadiamaps/ferrostar-core-react-native';
import { NavigationView } from '@stadiamaps/ferrostar-maplibre-react-native';
import { useMemo } from 'react';
import { useLocationPermission } from '@/hooks/useLocationPermissions';
import { useLocationTracker } from '@/hooks/useLocationTracker';

const apiKey = process.env.STADIA_MAPS_API_KEY ?? '';
const styleUrl = `https://tiles.stadiamaps.com/styles/alidade_smooth.json?api_key=${apiKey}`;

export default function Index() {
  const { isPermissionGranted } = useLocationPermission();
  const { currentPosition: location } = useLocationTracker();

  const core = useMemo(
    () =>
      new FerrostarCore('https://valhalla1.openstreetmap.de/route', 'auto', {
        waypointAdvance: new WaypointAdvanceMode.WaypointWithinRange(100.0),
        stepAdvanceCondition: stepAdvanceDistanceEntryAndExit(30, 5, 32),
        arrivalStepAdvanceCondition: stepAdvanceDistanceToEndOfStep(10, 32),
        routeDeviationTracking: new RouteDeviationTracking.StaticThreshold({
          minimumHorizontalAccuracy: 15,
          maxAcceptableDeviation: 50,
        }),
        snappedLocationCourseFiltering: CourseFiltering.SnapToRoute,
      }),
    []
  );

  const handleNavigationStart = async () => {
    if (!location) {
      return;
    }

    const { coords, timestamp } = location;
    const routes = await core.getRoutes(
      {
        coordinates: { lat: coords.latitude, lng: coords.longitude },
        horizontalAccuracy: coords.accuracy ?? 0,
        speed: undefined,
        courseOverGround: undefined,
        timestamp: new Date(timestamp),
      },
      [
        {
          coordinate: { lat: coords.latitude, lng: coords.longitude },
          kind: WaypointKind.Break,
        },
        {
          coordinate: {
            lat: -43.56823546768915,
            lng: 172.6902914460014,
          },
          kind: WaypointKind.Break,
        },
      ]
    );

    const route = routes[0];
    if (!route) {
      return;
    }
    console.log({ route });

    core.startNavigation(route);
    console.log(' Navigation started ');
  };

  if (!isPermissionGranted) {
    return (
      <View style={styles.container}>
        <Text>Location permission is required</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <NavigationView
        style={styles.container}
        mapStyle={styleUrl}
        core={core}
        snapUserLocationToRoute={true}
      />
      <Button title="Start Navigation" onPress={handleNavigationStart} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
