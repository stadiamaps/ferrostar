# Getting Started on the Web

This section of the guide covers how to integrate Ferrostar into a web app.
While there are limitations to the web [Geolocation API](https://developer.mozilla.org/en-US/docs/Web/API/Geolocation_API)
(notably no background updates),
PWAs and other mobile-optimized sites
can be a great solution when a native iOS/Android app is impractical or prohibitively expensive.

We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

## Add the package dependency

### Installing with `npm`

No surprises; just install with `npm` or any similar package manager.

```shell
npm install @stadiamaps/ferrostar-webcomponents
```

### Vite Setup

Vite currently has a few bundling issues with npm packages leveraging WASM.
We are hopeful that the [ES module integration proposal for WebAssembly](https://github.com/WebAssembly/esm-integration)
is eventually finalized and widely accepted,
but in the meantime there are some integration pains.

We currently recommend using [`vite-plugin-wasm`](https://github.com/Menci/vite-plugin-wasm?tab=readme-ov-file).
Add `vite-plugin-wasm` and `vite-plugin-top-level-await` to your `devDependencies`.
Then add `wasm()` and `topLevelAwait()` to the `plugins` section of your Vite config.

### Using unpkg

TODO

## Add Ferrostar web components to your web app

The Ferrostar web SDK uses the [Web Components](https://developer.mozilla.org/en-US/docs/Web/API/Web_components)
to ensure maximum compatibility across frontend frameworks.
You can import the components just like other things you’re used to in JavaScript.

```javascript
import { FerrostarMap, BrowserLocationProvider } from "@stadiamaps/ferrostar-webcomponents";
```

## Configure the `<ferrostar-map>` component

Now you can use Ferrostar in your HTML like this:

```html
<ferrostar-map
  id="ferrostar"
  valhallaEndpointUrl="https://api.stadiamaps.com/route/v1"
  styleUrl="https://tiles.stadiamaps.com/styles/outdoors.json"
  profile="bicycle"
></ferrostar-map>
```

Here we have used Stadia Maps URLs, which should work without authentication for local development.
(Refer to the [authentication docs](https://docs.stadiamaps.com/authentication/)
for network deployment details; you can start with a free account.)

See the [vendors appendix](./vendors.md) for a list of other compatible vendors.

`<ferrostar-map>`  additionally requires setting some CSS manually, or it will be invisible!

```css
ferrostar-map {
  display: block;
  width: 100%;
  height: 100%;
}
```

That’s all you need to get started!

### Configuration explained

`<ferrostar-map>` provides a few properties to configure.
Here are the most important ones:

- `valhallaEndpointUrl`: The Valhalla routing endpoint to use. You can use any reasonably up-to-date Valhalla server, including your own. See [vendors](./vendor.md#routing) for a list of known compatible vendors.
- `styleUrl`: The URL of the MapLibre style JSON for the map.
- `httpClient`: You can set your own fetch-compatible HTTP client to make requests to the routing API (ex: Valhalla).
- `options`: You can set the costing options for the route provider (ex: Valhalla JSON options).
- `locationProvider`: Provides locations to the navigation controller.
  `SimulatedLocationProvider` and `BrowserLocationProvider` are included.
  See the demo app for an example of how to simulate a route.
- `configureMap`: Configures the map on first load. This lets you customize the UI, add MapLibre map controls, etc. on load.
- `onNavigationStart`: Callback when navigation starts.
- `onNavigationStop`: Callback when navigation ends.
- `onTripStateChange`: Callback when the trip state changes (most state changes, such as a location update, advancing to the next step, etc.).
- `customStyles`: Custom CSS to load (the component uses a scoped shadow DOM; use this to load external styles).
- `useVoiceGuidance`: Enable voice guidance (default: `false`).
- `geolocateOnLoad`: Geolocate the user on load and zoom the map to their location (default: `true`; you probably want this unless you are simulating locations for testing).
- `customStyles`: Styles which will apply inside the component (ex: for MapLibre plugins).

If you haven’t worked with web components before,
one quick thing to understand is that the only thing you can configure
using *pure HTML* are string attributes.
Rich properties of any other type will not be properly passed through
if you are specifying HTML attributes!
If you’re using a vanilla framework, you will need to get the DOM object
and then set properties with JavaScript like so:

```javascript
const ferrostar = document.getElementById("ferrostar");

ferrostar.center = {lng: -122.42, lat: 37.81};
ferrostar.zoom = 18;
ferrostar.costingOptions = { bicycle: { use_roads: 0.2 } };
```

Other frameworks, like Vue, have better support for web components.
In Vue, you can write “markup” in your components like this!
However, there are a few gotchas.
The properties need to be written as camelCase, for one,
and some IDEs do not correctly suggest code completion.
You’ll also want to continue using JS (ex: via the `onMounted` hook in Vue)
for most complex properties like functions.

```javascript
<ferrostar-web
  id="ferrostar"
  valhallaEndpointUrl="https://api.stadiamaps.com/route/v1"
  styleUrl="https://tiles.stadiamaps.com/styles/outdoors.json"
  profile="bicycle"
  :center="{lng: -122.42, lat: 37.81}"
  :zoom="18"
  :useVoiceGuidance="true"
  :geolocateOnLoad="true"
></ferrostar-web>
```

NOTE: The JavaScript API is currently limited to Valhalla,
but support for arbitrary providers (like we already have on iOS and Android)
is [tracked in this issue](https://github.com/stadiamaps/ferrostar/issues/191).

## Acquiring the user’s location

The `BrowserLocationProvider` includes a convenience function
to get the user’s location asynchronously (using a cached one if available).
Use this to get the user’s location in the correct format.

```typescript
// Fetch the user's current location.
// If we have a cached one that's no older than 30 seconds,
// skip waiting for an update and use the slightly stale location.
const location = await ferrostar.locationProvider.getCurrentLocation(30_000);
```

## Getting routes

Once you have acquired the user’s location and have one or more waypoints to navigate to,
it’s time to get some routes!

```typescript
// Use the acquired user location to request the route
const routes = await ferrostar.getRoutes(location, waypoints);

// Select one of the routes; here we just pick the first one.
const route = routes[0];
```

## Starting navigation

Finally, we can start navigating!
We’re still working on getting full documentation generated in the typescript wrapper,
but in the meantime, the [Rust docs](https://docs.rs/ferrostar/latest/ferrostar/navigation_controller/models/struct.NavigationControllerConfig.html)
describe the available options.

```typescript
ferrostar.startNavigation(route, {
  stepAdvanceCondition: {
    DistanceEntryExit: {
      minimumHorizontalAccuracy: 25,
      distanceToEndOfStep: 30,
      distanceAfterEndStep: 5,
      hasReachedEndOfCurrentStep: false,
    },
  },
  arrivalStepAdvanceCondition: {
    DistanceToEndOfStep: {
      distance: 30,
      minimumHorizontalAccuracy: 25,
    },
  },
  routeDeviationTracking: {
    StaticThreshold: {
      minimumHorizontalAccuracy: 25,
      maxAcceptableDeviation: 10.0,
    },
  },
  snappedLocationCourseFiltering: "Raw",
  waypointAdvance: {
    WaypointWithinRange: 100,
  },
});
```

## Demo app

We've put together a minimal demo app with an example integration.
Check out the [source code](https://github.com/stadiamaps/ferrostar/tree/main/web/index.html)
or try the [hosted demo](https://stadiamaps.github.io/ferrostar/web-demo)
(works best from a phone if you want to use real geolocation).

## Going deeper

This covers the basic “batteries included” configuration and pre-built UI.
But there’s a lot of room for customization!
Skip on over to the customization chapters that interest you.
