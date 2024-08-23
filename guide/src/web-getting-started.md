# Getting Started on the Web

This section of the guide covers how to integrate Ferrostar into a web app.
We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

## Add the NPM package dependency

In your web app, you can add the Ferrostar NPM package as a dependency.
You will need to install [Rust](https://www.rust-lang.org/) and `wasm-pack` to build the NPM package.

```shell
cargo install wasm-pack
```

Then, in your web app, install the Ferrostar NPM package:

```shell
npm install /path/to/ferrostar/web
```

## Add Ferrostar web components to your app

Ferrostar web SDK is provided as web components.
To import the components, add the following line:

```javascript
import { FerrostarCore, BrowserLocationProvider } from "ferrostar-components";
```

### Route providers

You’ll need to decide on a route provider when you set up your `FerrostarCore` instance.
For limited testing, FOSSGIS maintains a public server with the URL `https://valhalla1.openstreetmap.de/route`.
For production use, you’ll need another solution like a [commercial vendor](./vendors.md)
or self-hosting.

### Map style providers

You can get a free Stadia Maps API key at https://client.stadiamaps.com
See https://stadiamaps.github.io/ferrostar/vendors.html for additional vendors.

Then, you can use the components in your HTML like this, for example:

```html
<ferrostar-core
  id="core"
  valhallaEndpointUrl="https://valhalla1.openstreetmap.de/route"
  styleUrl="https://tiles.stadiamaps.com/styles/outdoors.json?api_key=YOUR_API_KEY"
  profile="bicycle"
></ferrostar-core>
```

NOTE: `<ferrostar-core>` requires setting CSS manually or it will be invisible.

```css
ferrostar-core {
  display: block;
  width: 100%;
  height: 100%;
}
```

`<ferrostar-core>` web SDK contains an integrated search box, and you can already use it for navigation without any additional setup.

## Configure the `<ferrostar-core>` component

`<ferrostar-core>` provides a few properties to configure.
Here are the most important ones:

- `httpClient`: You can set your own fetch-compatible HTTP client to make requests to the Valhalla endpoint.
- `costingOptions`: You can set the costing options for the Ferrostar routing engine.
- `useIntegratedSearchBox`: You can disable the integrated search box and use your own code to handle navigation.

## Use your own code to handle navigation

If you want to use your own code to handle navigation instead of the integrated search box, you can do so by the following steps:

### (Optional) Implement your own search box

You can use this code to retrieve the latitude and longitude of a destination:

```javascript
const destination = "One Apple Park Way";

const { lat, lon } = await fetch("https://nominatim.openstreetmap.org/search?q=" + destination + "&format=json")
  .then((response) => response.json())
  .then((data) => data[0]);
```

### Configure the Ferrostar Core

(TODO)
Here's an example:

```javascript
const config = {
  stepAdvance: {
    RelativeLineStringDistance: {
      minimumHorizontalAccuracy: 25,
      automaticAdvanceDistance: 10,
    },
  },
  routeDeviationTracking: {
    StaticThreshold: {
      minimumHorizontalAccuracy: 25,
      maxAcceptableDeviation: 10.0,
    },
  },
};
```

### Getting a route

Before getting routes, you’ll need the user’s current location.
You can get this from the location provider.
`BrowserLocationProvider` is a location provider that uses the browser's geolocation API.

```javascript
// Request location permission and start location updates
const locationProvider = new BrowserLocationProvider();
locationProvider.requestPermission();
locationProvider.start();

// TODO: This approach is not ideal, any better way to wait for the locationProvider to acquire the first location?
while (!locationProvider.lastLocation) {
  await new Promise((resolve) => setTimeout(resolve, 100));
}
```

Next, you’ll need a set of waypoints to visit.

```javascript
const waypoints = [{ coordinate: { lat: parseFloat(lat), lng: parseFloat(lon) }, kind: "Break" }];
```

Finally, you can use the asynchronous `getRoutes` method on `FerrostarCore`.
Here’s an example:

```javascript
const core = document.getElementById("core");
const routes = await core.getRoutes(locationProvider.lastLocation, waypoints);
const route = routes[0];
```

### Start the navigation

Once you or the user has selected a route, it’s time to start navigating!

```javascript
core.locationProvider = locationProvider;
core.startNavigation(route, config);
```

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/web/index.html) with an example integration.

## Going deeper

This covers the basic “batteries included” configuration and pre-built UI.
But there’s a lot of room for customization!
Skip on over to the customization chapters that interest you.