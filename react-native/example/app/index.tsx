import { StyleSheet, View, Button, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  WaypointKind,
  UserLocation,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import {
  useFerrostar,
  SimulatedLocationProvider,
} from '@stadiamaps/ferrostar-core-react-native';
import {
  NavigationMap,
  NotNavigating,
} from '@stadiamaps/ferrostar-maplibre-react-native';
import { useEffect } from 'react';
import { useLocationPermission } from '../hooks/useLocationPermissions';
import { useLocationTracker } from '../hooks/useLocationTracker';
import {
  AutocompleteSearchInput,
  AutocompleteSearchResults,
  AutocompleteSearchRoot,
} from '@/components/auto-complete-search';
import { Configuration, FeaturePropertiesV2 } from '@stadiamaps/api';

const apiKey = process.env.EXPO_PUBLIC_STADIA_MAPS_API_KEY ?? '';
const styleUrl = `https://tiles.stadiamaps.com/styles/outdoors.json?api_key=${apiKey}`;
const config = new Configuration({ apiKey });

export default function Index() {
  const { isPermissionGranted } = useLocationPermission();
  const { currentPosition: location } = useLocationTracker();

  const core = useFerrostar();

  useEffect(() => {
    if (!location) {
      return;
    }
    const { coords, timestamp } = location;
    const userLocation = {
      coordinates: { lat: coords.latitude, lng: coords.longitude },
      horizontalAccuracy: coords.accuracy ?? 0,
      courseOverGround: undefined,
      timestamp: new Date(timestamp),
      speed:
        coords.speed !== null
          ? { value: coords.speed, accuracy: undefined }
          : undefined,
    };

    if (core.locationProvider instanceof SimulatedLocationProvider) {
      core.locationProvider.updateLocation(
        userLocation as unknown as UserLocation
      );
    }
  }, [location, core]);

  const handleNavigationStart = async (result: FeaturePropertiesV2 | null) => {
    if (!location || !result || !result.geometry) {
      return;
    }

    const [endLng, endLat] = result.geometry.coordinates;
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
          coordinate: {
            lat: endLat,
            lng: endLng,
          },
          kind: WaypointKind.Break,
        },
      ]
    );

    const route = routes[0];
    if (!route) {
      return;
    }
    const userLocation = {
      coordinates: { lat: coords.latitude, lng: coords.longitude },
      horizontalAccuracy: coords.accuracy ?? 0,
      speed: undefined,
      courseOverGround: undefined,
      timestamp: new Date(timestamp),
    };
    if (core.locationProvider instanceof SimulatedLocationProvider) {
      core.locationProvider.updateLocation(
        userLocation as unknown as UserLocation
      );
    }
    core.startNavigation(route);
    if (core.locationProvider instanceof SimulatedLocationProvider) {
      core.locationProvider.setRoute(route);
    }
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
      <NotNavigating>
        <AutocompleteSearchRoot
          userLocation={{
            lat: location?.coords.latitude ?? 0,
            lng: location?.coords.longitude ?? 0,
          }}
          config={config}
          style={{
            padding: 10,
          }}
          onResultSelected={handleNavigationStart}
        >
          <AutocompleteSearchInput />
          <AutocompleteSearchResults />
        </AutocompleteSearchRoot>
      </NotNavigating>
      <NavigationMap
        style={styles.container}
        mapStyle={styleUrl}
        onStopNavigation={() => {
          if (core.locationProvider instanceof SimulatedLocationProvider) {
            core.locationProvider.stop();
          }
        }}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    position: 'relative',
  },
});
