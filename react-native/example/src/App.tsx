import { Button, StyleSheet, View } from 'react-native';
import {
  CourseFiltering,
  FerrostarCore,
  RouteDeviationTracking,
  SpecialAdvanceConditions,
  StepAdvanceMode,
  WaypointKind,
} from 'react-native-ferrostar/core';
import { NavigationView } from 'react-native-ferrostar/views';
import Geolocation, {
  type GeolocationResponse,
} from '@react-native-community/geolocation';
import { useEffect, useMemo, useState } from 'react';

const apiKey = process.env.STADIA_MAPS_API_KEY ?? '';
const styleUrl = `https://tiles.stadiamaps.com/styles/alidade_smooth.json?api_key=${apiKey}`;

export default function App() {
  const [location, setLocation] = useState<GeolocationResponse | null>(null);

  const core = useMemo(
    () =>
      new FerrostarCore('https://valhalla1.openstreetmap.de/route', 'auto', {
        stepAdvance: StepAdvanceMode.RelativeLineStringDistance.new({
          minimumHorizontalAccuracy: 25,
          specialAdvanceConditions:
            SpecialAdvanceConditions.MinimumDistanceFromCurrentStepLine.new(10),
        }),
        routeDeviationTracking: RouteDeviationTracking.StaticThreshold.new({
          minimumHorizontalAccuracy: 15,
          maxAcceptableDeviation: 50,
        }),
        snappedLocationCourseFiltering: CourseFiltering.SnapToRoute,
      }),
    []
  );

  useEffect(() => {
    Geolocation.requestAuthorization();

    if (location === null) {
      Geolocation.getCurrentPosition(
        (l) => {
          setLocation(l);
        },
        (e) => {
          console.log(e);
        },
        {
          enableHighAccuracy: true,
          fastestInterval: 1000,
          interval: 1000,
        }
      );
    }

    const watchId = Geolocation.watchPosition(
      (l) => {
        setLocation(l);
      },
      (e) => {
        console.log(e);
      },
      {
        enableHighAccuracy: true,
        fastestInterval: 0,
        interval: 1000,
      }
    );

    return () => {
      Geolocation.clearWatch(watchId);
    };
  }, [location]);

  const handleNavigationStart = async () => {
    if (!location) {
      return;
    }

    const { coords, timestamp } = location;
    const routes = await core.getRoutes(
      {
        coordinates: { lat: coords.latitude, lng: coords.longitude },
        horizontalAccuracy: coords.accuracy,
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

    core.startNavigation(route);
  };

  return (
    <View style={styles.container}>
      <NavigationView
        style={styles.container}
        mapStyle={styleUrl}
        core={core}
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
