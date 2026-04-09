import { StyleSheet, View, Button, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  CourseFiltering,
  RouteDeviationTracking,
  stepAdvanceDistanceEntryAndExit,
  stepAdvanceDistanceToEndOfStep,
  WaypointAdvanceMode,
  WaypointKind,
  WellKnownRouteProvider,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import { FerrostarCore } from '@stadiamaps/ferrostar-core-react-native';
import { NavigationView } from '@stadiamaps/ferrostar-maplibre-react-native';
import { useMemo } from 'react';
import { useLocationPermission } from '@/hooks/useLocationPermissions';
import { useLocationTracker } from '@/hooks/useLocationTracker';
import { withJsonOptions } from '@stadiamaps/ferrostar-core-react-native/src/RouteProvider';

const apiKey = process.env.EXPO_PUBLIC_STADIA_MAPS_API_KEY ?? '';
const styleUrl = `https://tiles.stadiamaps.com/styles/outdoors.json?api_key=${apiKey}`;

export default function Index() {
  const { isPermissionGranted } = useLocationPermission();
  const { currentPosition: location } = useLocationTracker();

  const core = useMemo(
    () =>
      new FerrostarCore(
        {
          waypointAdvance: new WaypointAdvanceMode.WaypointWithinRange(100.0),
          stepAdvanceCondition: stepAdvanceDistanceEntryAndExit(30, 5, 32),
          arrivalStepAdvanceCondition: stepAdvanceDistanceToEndOfStep(10, 32),
          routeDeviationTracking: new RouteDeviationTracking.StaticThreshold({
            minimumHorizontalAccuracy: 15,
            maxAcceptableDeviation: 50,
          }),
          snappedLocationCourseFiltering: CourseFiltering.SnapToRoute,
        },
        undefined,
        {
          kind: 'adapter',
          provider: withJsonOptions(
            WellKnownRouteProvider.Valhalla.new({
              endpointUrl: 'https://valhalla1.openstreetmap.de/route',
              profile: 'auto',
              optionsJson: undefined,
            })
          ),
        }
      ),
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
    <SafeAreaView style={styles.container}>
      <NavigationView
        style={styles.container}
        mapStyle={styleUrl}
        core={core}
        snapUserLocationToRoute={true}
      />
      <Button title="Start Navigation" onPress={handleNavigationStart} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
