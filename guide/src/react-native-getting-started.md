# Getting Started with React Native

This section of the guide covers how to integrate Ferrostar into a React Native app.
We'll use the core package with the MapLibre-based navigation UI,
but you can use the core independently and build your own interface.

The React Native SDK contains three packages:

- `@stadiamaps/ferrostar-uniffi-react-native` contains the generated Rust bindings and data models.
- `@stadiamaps/ferrostar-core-react-native` manages routing, location updates, and navigation state.
- `@stadiamaps/ferrostar-maplibre-react-native` provides the MapLibre map and navigation UI.

## Add the package dependencies

Install the three Ferrostar packages and the native dependencies used by the MapLibre UI.

```shell
npm install \
  @stadiamaps/ferrostar-uniffi-react-native \
  @stadiamaps/ferrostar-core-react-native \
  @stadiamaps/ferrostar-maplibre-react-native \
  @maplibre/maplibre-react-native \
  react-native-svg
```

You can use another package manager if you prefer.

Ferrostar and MapLibre include native code,
so an Expo app must use a [development build](https://docs.expo.dev/develop/development-builds/introduction/)
rather than Expo Go.
After adding the dependencies,
regenerate and build the native projects using the workflow appropriate for your app.

For an Expo app:

```shell
npx expo prebuild
npx expo run:ios
# or
npx expo run:android
```

For a bare React Native iOS app,
install the CocoaPods dependencies before rebuilding the app.

```shell
cd ios
pod install
```

## Provide location updates

Ferrostar does not request location permission or select a React Native location library for you.
Use the permission and location APIs provided by your chosen library,
then forward its location updates to Ferrostar.
For example,
Expo apps can use [`expo-location`](https://docs.expo.dev/versions/latest/sdk/location/).

### Connect a location provider

The core consumes locations through the `LocationProvider` interface.
`ManualLocationProvider` is useful when another library already supplies locations to your app.
Create the provider once and forward each update to it.

```typescript
import {useCallback, useMemo} from 'react';
import {ManualLocationProvider} from '@stadiamaps/ferrostar-core-react-native';
import type {UserLocation} from '@stadiamaps/ferrostar-uniffi-react-native';

export function useFerrostarLocationProvider() {
  const locationProvider = useMemo(() => new ManualLocationProvider(), []);

  // Call this whenever your location library reports a new position.
  const updateLocation = useCallback(
    (location: UserLocation) => locationProvider.updateLocation(location),
    [locationProvider]
  );

  return {locationProvider, updateLocation};
}
```

Pass the returned `locationProvider` to `FerrostarProvider`,
and call `updateLocation` from your location library's subscription callback.

A `UserLocation` uses latitude and longitude in degrees,
horizontal accuracy in meters,
and a JavaScript `Date` for its timestamp.
For example,
an update without optional speed or course information looks like this:

```typescript
updateLocation({
  coordinates: {
    lat: position.coords.latitude,
    lng: position.coords.longitude,
  },
  horizontalAccuracy: position.coords.accuracy ?? 0,
  courseOverGround: undefined,
  speed: undefined,
  timestamp: new Date(position.timestamp),
});
```

Keep the provider instance stable across React renders.
`FerrostarProvider` subscribes to it when mounted and cleans up that subscription when unmounted.

For testing without moving a device,
use `SimulatedLocationProvider` instead.
After selecting a route,
call `setRoute(route)` to simulate progress along it.

### Integrate foreground navigation

Ferrostar does not depend on a particular background-location or notification library.
Instead,
provide a `ForegroundService` adapter that delegates to the APIs chosen by your app.
The adapter starts with navigation,
receives navigation state updates,
and stops when the trip stops or `FerrostarProvider` unmounts.

```typescript
import {useMemo} from 'react';
import type {
  ForegroundService,
  NavigationState,
} from '@stadiamaps/ferrostar-core-react-native';

function useForegroundService(): ForegroundService {
  return useMemo(
    () => ({
      start: ({stopNavigation}) =>
        platformNavigation.start({onStop: stopNavigation}),
      update: (state: NavigationState) =>
        platformNavigation.update(state.tripState),
      stop: () => platformNavigation.stop(),
    }),
    []
  );
}
```

Here, `platformNavigation` represents your application integration.
It can wrap an Android foreground service,
an iOS Live Activity for presenting trip status,
or foreground behavior supplied by your location library.
The `update` method is optional when the platform integration does not display navigation state.

Pass the stable adapter to the provider:

```typescript
const foregroundService = useForegroundService();

return (
  <FerrostarProvider
    config={config}
    foregroundService={foregroundService}
    locationProvider={locationProvider}
    routeProvider={routeProvider}
  >
    {children}
  </FerrostarProvider>
);
```

The adapter methods may be synchronous or return promises.
Ferrostar executes them in order and reports failures to the console
without interrupting navigation.
Your application remains responsible for any permissions,
native configuration,
and background modes required by the chosen platform API.
The adapter coordinates lifecycle events;
it does not itself grant background execution.

## Configure Ferrostar

Wrap the part of your component tree that uses Ferrostar in `FerrostarProvider`.
The provider owns a stable `FerrostarCore` instance and makes it available through hooks.

The following example uses a Valhalla route provider.
See the [routing and basemap vendors](./vendors.md) page for compatible services,
or implement a custom route provider for another service or an offline router.

```typescript
import {useMemo, type PropsWithChildren} from 'react';
import {
  FerrostarProvider,
  ManualLocationProvider,
  type RouteProvider,
} from '@stadiamaps/ferrostar-core-react-native';
import {
  CameraProvider,
} from '@stadiamaps/ferrostar-maplibre-react-native';
import {
  CourseFiltering,
  RouteDeviationTracking,
  stepAdvanceDistanceEntryAndExit,
  stepAdvanceDistanceToEndOfStep,
  WaypointAdvanceMode,
  WellKnownRouteProvider,
} from '@stadiamaps/ferrostar-uniffi-react-native';

const endpointUrl =
  'https://api.stadiamaps.com/route/v1?api_key=YOUR-API-KEY';

const config = {
  waypointAdvance: new WaypointAdvanceMode.WaypointWithinRange(100),
  stepAdvanceCondition: stepAdvanceDistanceEntryAndExit(30, 5, 32),
  arrivalStepAdvanceCondition: stepAdvanceDistanceToEndOfStep(10, 32),
  routeDeviationTracking: new RouteDeviationTracking.StaticThreshold({
    minimumHorizontalAccuracy: 15,
    maxAcceptableDeviation: 50,
  }),
  snappedLocationCourseFiltering: CourseFiltering.SnapToRoute,
};

const routeProvider: RouteProvider = {
  kind: 'adapter',
  provider: WellKnownRouteProvider.Valhalla.new({
    endpointUrl,
    profile: 'auto',
    optionsJson: undefined,
  }),
};

export function NavigationProviders({children}: PropsWithChildren) {
  const locationProvider = useMemo(() => new ManualLocationProvider(), []);

  return (
    <FerrostarProvider
      config={config}
      locationProvider={locationProvider}
      routeProvider={routeProvider}
    >
      <CameraProvider>{children}</CameraProvider>
    </FerrostarProvider>
  );
}
```

The example values are starting points,
not universal recommendations.
See [Configuring the Navigation Controller](./configuring-the-navigation-controller.md)
for an explanation of step advancement, waypoint advancement, and route deviation tracking.

## Get a route

Components beneath `FerrostarProvider` can access the core with `useFerrostar`.
Once you have a current location and a destination,
request routes and select one to navigate.

```typescript
import {useFerrostar} from '@stadiamaps/ferrostar-core-react-native';
import {WaypointKind} from '@stadiamaps/ferrostar-uniffi-react-native';

const core = useFerrostar();

const routes = await core.getRoutes(currentLocation, [
  {
    coordinate: {
      lat: destination.latitude,
      lng: destination.longitude,
    },
    kind: WaypointKind.Break,
  },
]);

const route = routes[0];
if (!route) {
  // Tell the user that the routing service returned no route.
  return;
}

core.startNavigation(route);
```

Route requests can fail because of network errors,
authentication errors,
invalid waypoints,
or a routing service that cannot find a route.
Catch these errors and present an appropriate state to the user.

## Add the navigation UI

`NavigationMap` provides the route line,
navigation camera,
location puck,
instruction banner,
map controls,
and trip progress UI.
It must be rendered beneath both `FerrostarProvider` and `CameraProvider`.

```typescript
import {StyleSheet} from 'react-native';
import {NavigationMap} from '@stadiamaps/ferrostar-maplibre-react-native';

const styleUrl =
  'https://tiles.stadiamaps.com/styles/outdoors.json?api_key=YOUR-API-KEY';

export function NavigationScreen() {
  return <NavigationMap style={styles.map} mapStyle={styleUrl} />;
}

const styles = StyleSheet.create({
  map: {
    flex: 1,
  },
});
```

By default, `NavigationMap` dynamically switches between portrait and landscape layouts
as the window dimensions change.
Set `layout="portrait"` or `layout="landscape"` to keep a static layout instead.

You can also compose the exported UI components individually
when the prebuilt map does not fit your design.

## Observe and stop navigation

Use `useNavigationState` when your own components need to react to navigation changes.
The hook manages the core listener lifecycle and returns a UI-oriented state object.

```typescript
import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';

const core = useFerrostar();
const navigationState = useNavigationState(core);

function stopNavigation() {
  core.stopNavigation();
}
```

## Demo app

The repository includes an Expo demo app with route search,
location permission handling,
voice guidance,
route simulation,
and the MapLibre navigation UI.
See the [React Native example source](https://github.com/stadiamaps/ferrostar/tree/main/react-native/example)
for a complete integration.

## Going deeper

This chapter introduces the core integration and prebuilt UI.
For more control,
implement the `LocationProvider`, `RouteProvider`, or `SpeechEngine` interfaces,
or compose the individual components exported by the MapLibre UI package.
See [Session Recording](./session-recording.md)
for platform availability and current limitations.
