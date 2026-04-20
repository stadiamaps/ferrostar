import { SimulatedLocationProvider } from '@stadiamaps/ferrostar-core-react-native';
import { FerrostarProvider } from '@stadiamaps/ferrostar-core-react-native/src/contexts/FerrostarProvider';
import { withJsonOptions } from '@stadiamaps/ferrostar-core-react-native/src/RouteProvider';
import { SpeechEngine } from '@stadiamaps/ferrostar-core-react-native/src/SpeechEngine';
import { CameraProvider } from '@stadiamaps/ferrostar-maplibre-react-native/src/contexts/CameraProvider';
import {
  CourseFiltering,
  RouteDeviationTracking,
  stepAdvanceDistanceEntryAndExit,
  stepAdvanceDistanceToEndOfStep,
  WaypointAdvanceMode,
  WellKnownRouteProvider,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import { Stack } from 'expo-router';
import * as Speech from 'expo-speech';
import { useEffect, useMemo } from 'react';

const endpointUrl = process.env.EXPO_PUBLIC_ENDPOINT_URL ?? '';

const config = {
  waypointAdvance: new WaypointAdvanceMode.WaypointWithinRange(100.0),
  stepAdvanceCondition: stepAdvanceDistanceEntryAndExit(30, 5, 32),
  arrivalStepAdvanceCondition: stepAdvanceDistanceToEndOfStep(10, 32),
  routeDeviationTracking: new RouteDeviationTracking.StaticThreshold({
    minimumHorizontalAccuracy: 15,
    maxAcceptableDeviation: 50,
  }),
  snappedLocationCourseFiltering: CourseFiltering.SnapToRoute,
};

const routeProvider = {
  kind: 'adapter' as const,
  provider: withJsonOptions(
    WellKnownRouteProvider.Valhalla.new({
      endpointUrl,
      profile: 'auto',
      optionsJson: undefined,
    })
  ),
};

export default function RootLayout() {
  const locationProvider = useMemo(() => new SimulatedLocationProvider(), []);

  useEffect(() => {
    return () => {
      locationProvider.stop();
    };
  }, [locationProvider]);

  const speechEngine = useMemo(
    () => ({
      speak: (text: string) => Speech.speak(text),
      stop: () => Speech.stop(),
    }),
    []
  );

  return (
    <FerrostarProvider
      config={config}
      locationProvider={locationProvider}
      routeProvider={routeProvider}
      speechEngine={speechEngine}
    >
      <CameraProvider>
        <Stack screenOptions={{ headerShown: false }} />
      </CameraProvider>
    </FerrostarProvider>
  );
}
