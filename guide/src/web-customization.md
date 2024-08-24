# Web

The [web tutorial](./web-getting-started.md) gets you set up with a “batteries included” UI
and sane defaults (if a bit customized for Stadia Maps at the moment).
This document covers ways you can customize it to your needs.

## Removing or replacing the integrated search box

If you want to use your own code to handle navigation instead of the integrated search box, you can do so by the following steps:

### Disable the integrated search box

You can disable the integrated search box with the `useIntegratedSearchBox` attribute.

```html
<ferrostar-core
  id="core"
  valhallaEndpointUrl="https://api.stadiamaps.com/route/v1"
  styleUrl="https://tiles.stadiamaps.com/styles/outdoors.json"
  profile="bicycle"
  useIntegratedSearchBox="false"
></ferrostar-core>
```

### Use your own search box/geocoding API

The HTML, JS, and CSS for this is out of scope of this guide,
but here’s an example (without UI)
showing how to retrieve the latitude and longitude of a destination
using the Nominatim API ([note their usage policy](https://operations.osmfoundation.org/policies/nominatim/) before deploying):

```javascript
const destination = "One Apple Park Way";

const { lat, lon } = await fetch("https://nominatim.openstreetmap.org/search?q=" + destination + "&format=json")
  .then((response) => response.json())
  .then((data) => data[0]);
```

### Get routes manually

Once you have your waypoint(s) geocoded,
create a list of them like this:

```javascript
const waypoints = [{ coordinate: { lat: parseFloat(lat), lng: parseFloat(lon) }, kind: "Break" }];
```

The asynchronous `getRoutes` method on `FerrostarCore`
will fetch routes from your route provider (ex: a Valhalla server).
Here’s an example:

```javascript
const core = document.getElementById("core");
const routes = await core.getRoutes(locationProvider.lastLocation, waypoints);
const route = routes[0];
```

### Starting navigation manually

Once you have a route,
it’s time to start navigating!

```javascript
core.startNavigation(route, config);
```

## Location providers

The “batteries include” defaults will use the web Geolocation API automatically.
However, you can override this for simulation purposes.

### `BrowserLocationProvider`

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

### `SimulatedLocationProvider`

TODO: Documentation